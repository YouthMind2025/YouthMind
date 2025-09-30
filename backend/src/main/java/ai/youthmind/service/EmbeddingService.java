package ai.youthmind.service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.youthmind.config.EmbeddingConfig;
import ai.youthmind.config.EmbeddingConfig.*;
import ai.youthmind.config.OnnxConfig;
import ai.youthmind.dto.SimilarityValidationResultDto;
import ai.youthmind.dto.SimilarityValidationResultDto.SimilarityTestResult;
import ai.youthmind.dto.TokenizationResultDto;
import ai.youthmind.dto.ValidationResultDto.ValidationCheck;
import ai.youthmind.dto.ValidationResultDto;
import ai.youthmind.util.EmbeddingUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.nio.IntBuffer;
import java.util.*;

@Service
public class EmbeddingService {

    @Autowired
    private OrtEnvironment environment;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private TokenizerService tokenizerService;

    @Autowired
    private EmbeddingConfig embeddingConfig;

    @Cacheable(value = "embeddings", keyGenerator = "embeddingKeyGenerator")
    public double[] generateEmbedding(String version, String text, boolean forRetrieval) throws Exception {
        TokenizationResultDto result = tokenizerService.tokenize(version, text, forRetrieval);

        // Prepare inputs for ONNX model
        Map<String, OnnxTensor> inputs = prepareModelInputs(result);

        // Run inference
        OrtSession session = OnnxConfig.ortSession(getModelConfig(version).getPath(), environment, resourceLoader);
        try (OrtSession.Result results = session.run(inputs)) {
            return extractEmbedding(version, results);
        }
    }

    private Map<String, OnnxTensor> prepareModelInputs(TokenizationResultDto tokenization) throws Exception {
        Map<String, OnnxTensor> inputs = new HashMap<>();

        // Input IDs
        int[] tokenIds = tokenization.getTokenIds();
        long[] tokenIdsShape = {1, tokenIds.length};
        OnnxTensor inputIdsTensor = OnnxTensor.createTensor(
                environment, IntBuffer.wrap(tokenIds), tokenIdsShape);
        inputs.put("input_ids", inputIdsTensor);

        // Attention Mask
        int[] attentionMask = tokenization.getAttentionMask();
        long[] attentionMaskShape = {1, attentionMask.length};
        OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(
                environment, IntBuffer.wrap(attentionMask), attentionMaskShape);
        inputs.put("attention_mask", attentionMaskTensor);

        // Token Type IDs (for models that require them)
        int[] tokenTypeIds = new int[tokenization.getTokenIds().length];
        Arrays.fill(tokenTypeIds, 0); // Single sequence
        long[] tokenTypeIdsShape = {1, tokenTypeIds.length};
        OnnxTensor tokenTypeIdsTensor = OnnxTensor.createTensor(
                environment, IntBuffer.wrap(tokenTypeIds), tokenTypeIdsShape);
        inputs.put("token_type_ids", tokenTypeIdsTensor);

        return inputs;
    }

    private double[] extractEmbedding(String version, OrtSession.Result results) throws OrtException {
        // extract the [CLS] token embedding for sentence representation

        OnnxTensor outputTensor = (OnnxTensor) results.get(0);
        double[][][] hiddenStates = (double[][][]) outputTensor.getValue();

        // Shape: [batch_size, sequence_length, hidden_size]
        // Extract [CLS] token embedding (first token)
        double[] embedding = hiddenStates[0][0];

        if (getModelConfig(version).isNormalize()) {
            embedding = normalize(embedding);
        }
        return embedding;
    }

    public ModelConfig getModelConfig(String version) {
        if (embeddingConfig.getModels().containsKey(version)) {
            return embeddingConfig.getModels().get(version);
        }
        String defaultVersion = embeddingConfig.getVersioningConfig().getDefaultVersion();
        return embeddingConfig.getModels().get(defaultVersion);
    }

    public Map<String, Object> getModelInfo(String version) {
        ModelConfig modelConfig = getModelConfig(version);

        Map<String, Object> info = new HashMap<>();
        info.put("model_name", modelConfig.getName());
        info.put("max_length", modelConfig.getMaxLength());
        info.put("normalize", modelConfig.isNormalize());
        info.put("tokenizer_config", tokenizerService.getTokenizerConfig(version));

        return info;
    }

    private double[] normalize(double[] vector) {
        float norm = (float) Math.sqrt(Arrays.stream(vector).map(x -> x * x).sum());
        return Arrays.stream(vector).map(x -> x / norm).toArray();
    }

    public ValidationResultDto validateEmbedding(double[] embedding, String text) {
        List<ValidationCheck> checks = new ArrayList<>();

        // Basic quality checks
        checks.add(validateEmbeddingNorm(embedding));
        checks.add(validateZeroRatio(embedding));
        checks.add(validateNanValues(embedding));
        checks.add(validateVariance(embedding));

        boolean allPassed = checks.stream().allMatch(ValidationCheck::isPassed);

        return ValidationResultDto.builder()
                .text(text)
                .checks(checks)
                .overallPassed(allPassed)
                .embeddingDimension(embedding.length)
                .build();
    }

    private ValidationCheck validateEmbeddingNorm(double[] embedding) {
        ValidationConfig validationConfig = embeddingConfig.getValidationConfig();
        double norm = EmbeddingUtil.calculateNorm(embedding);
        boolean passed = norm >= validationConfig.getQualityThresholds().getMinNorm() &&
                norm <= validationConfig.getQualityThresholds().getMaxNorm();

        return ValidationCheck.builder()
                .checkName("Embedding Norm")
                .passed(passed)
                .actualValue(norm)
                .expectedRange(String.format("[%.2f, %.2f]",
                        validationConfig.getQualityThresholds().getMinNorm(),
                        validationConfig.getQualityThresholds().getMaxNorm()))
                .message(passed ? "Norm is within acceptable range" : "Norm is outside acceptable range")
                .build();
    }

    private ValidationCheck validateZeroRatio(double[] embedding) {
        ValidationConfig validationConfig = embeddingConfig.getValidationConfig();
        double zeroRatio = EmbeddingUtil.calculateZeroRatio(embedding);
        boolean passed = zeroRatio <= validationConfig.getQualityThresholds().getMaxZeroRatio();

        return ValidationCheck.builder()
                .checkName("Zero Value Ratio")
                .passed(passed)
                .actualValue(zeroRatio)
                .expectedRange(String.format("<= %.2f", validationConfig.getQualityThresholds().getMaxZeroRatio()))
                .message(passed ? "Zero ratio is acceptable" : "Too many zero values in embedding")
                .build();
    }

    private ValidationCheck validateNanValues(double[] embedding) {
        ValidationConfig validationConfig = embeddingConfig.getValidationConfig();
        double nanRatio = EmbeddingUtil.calculateNanRatio(embedding);
        boolean passed = nanRatio <= validationConfig.getQualityThresholds().getMaxNanRatio();

        return ValidationCheck.builder()
                .checkName("NaN Values")
                .passed(passed)
                .actualValue(nanRatio)
                .expectedRange(String.format("<= %.2f", validationConfig.getQualityThresholds().getMaxNanRatio()))
                .message(passed ? "No NaN values detected" : "NaN values found in embedding")
                .build();
    }

    private ValidationCheck validateVariance(double[] embedding) {
        double variance = EmbeddingUtil.calculateVariance(embedding);
        boolean passed = variance > 0.001 && variance < 1.0;

        return ValidationCheck.builder()
                .checkName("Value Variance")
                .passed(passed)
                .actualValue(variance)
                .expectedRange("(0.001, 1.0)")
                .message(passed ? "Variance is reasonable" : "Variance is outside expected range")
                .build();
    }

    public SimilarityValidationResultDto validateSimilarityBehavior(String version) throws Exception {
        List<SimilarityTestResult> similarityResults = new ArrayList<>();
        List<SimilarityTestResult> dissimilarityResults = new ArrayList<>();

        // Test similar pairs
        ValidationConfig validationConfig = embeddingConfig.getValidationConfig();
        for (String pair : validationConfig.getTestCases().getSimilarityPairs()) {
            String[] texts = pair.split("\\|");
            if (texts.length == 2) {
                double[] emb1 = generateEmbedding(version, texts[0], true);
                double[] emb2 = generateEmbedding(version, texts[1], true);
                double similarity = EmbeddingUtil.cosineSimilarity(emb1, emb2);

                similarityResults.add(SimilarityTestResult.builder()
                        .text1(texts[0])
                        .text2(texts[1])
                        .similarity(similarity)
                        .passed(similarity >= validationConfig.getQualityThresholds().getMinCosineSimilarity())
                        .build());
            }
        }

        // Test dissimilar pairs
        for (String pair : validationConfig.getTestCases().getDissimilarityPairs()) {
            String[] texts = pair.split("\\|");
            if (texts.length == 2) {
                double[] emb1 = generateEmbedding(version, texts[0], true);
                double[] emb2 = generateEmbedding(version, texts[1], true);
                double similarity = EmbeddingUtil.cosineSimilarity(emb1, emb2);

                dissimilarityResults.add(SimilarityTestResult.builder()
                        .text1(texts[0])
                        .text2(texts[1])
                        .similarity(similarity)
                        .passed(similarity < validationConfig.getQualityThresholds().getMinCosineSimilarity())
                        .build());
            }
        }

        return SimilarityValidationResultDto.builder()
                .similarityTests(similarityResults)
                .dissimilarityTests(dissimilarityResults)
                .similarityThreshold(validationConfig.getQualityThresholds().getMinCosineSimilarity())
                .build();
    }
}
