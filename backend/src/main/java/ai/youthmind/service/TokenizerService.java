package ai.youthmind.service;

import ai.youthmind.config.EmbeddingConfig;
import ai.youthmind.config.EmbeddingConfig.*;
import ai.youthmind.dto.TokenizationResultDto;
import ai.youthmind.dto.TokenizerDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class TokenizerService {

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmbeddingConfig embeddingConfig;

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("\\p{P}");
    private static final Pattern CHINESE_CHAR_PATTERN = Pattern.compile("[\u4e00-\u9fff]");
    private static final Pattern CLEAN_PATTERN = Pattern.compile("[^a-zA-Z0-9\\s\\p{P}\u4e00-\u9fff]");

    private static final String UNK_TOKEN = "[UNK]";
    private static final String PAD_TOKEN = "[PAD]";
    private static final String CLS_TOKEN = "[CLS]";
    private static final String SEP_TOKEN = "[SEP]";
    private static final String MASK_TOKEN = "[MASK]";

    private final Map<String, TokenizerDto> tokenizerDtos = new HashMap<>();

    @PostConstruct
    public void init() throws IOException {
        Map<String, ModelConfig> models = embeddingConfig.getModels();
        for (String version : models.keySet()) {
            try {
                ModelConfig model = models.get(version);

                // Load vocabulary
                Map<String, Integer> vocab = new HashMap<>();
                Resource vocabResource = resourceLoader.getResource(model.getVocabFile());
                String vocabContent = FileCopyUtils.copyToString(
                        new InputStreamReader(vocabResource.getInputStream(), StandardCharsets.UTF_8));

                String[] lines = vocabContent.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    vocab.put(lines[i].trim(), i);
                }

                // Add special tokens if not in vocab
                for (String token : new String[]{UNK_TOKEN, PAD_TOKEN, CLS_TOKEN, SEP_TOKEN, MASK_TOKEN}) {
                    if (!vocab.containsKey(token)) {
                        vocab.put(token, vocab.size());
                    }
                }

                // Load tokenizer config
                Resource configResource = resourceLoader.getResource(model.getTokenizerConfig());
                String configContent = FileCopyUtils.copyToString(
                        new InputStreamReader(configResource.getInputStream(), StandardCharsets.UTF_8));
                Map<String, Object> config = objectMapper.readValue(configContent, HashMap.class);

                tokenizerDtos.put(version, new TokenizerDto(vocab, config));
            } catch (Exception e) {
                tokenizerDtos.put(version, new TokenizerDto());
            }
        }
    }

    public TokenizationResultDto tokenize(String text, String version, boolean forRetrieval) {
        ModelConfig modelConfig = embeddingConfig.getModels().get(version);
        TokenizerDto tokenizerDto = tokenizerDtos.get(version);
        Map<String, Integer> vocab = tokenizerDto.getVocab();
        String processedText = preprocessText(text, forRetrieval);

        // Basic tokenization (split into words/punctuation)
        List<String> basicTokens = basicTokenize(processedText);

        // Apply WordPiece tokenization
        List<String> wordPieceTokens = new ArrayList<>();
        List<Integer> tokenIds = new ArrayList<>();

        // Add [CLS] token at the beginning
        wordPieceTokens.add(CLS_TOKEN);
        tokenIds.add(vocab.get(CLS_TOKEN));

        for (String token : basicTokens) {
            List<String> subTokens = wordPieceTokenize(vocab, token);
            wordPieceTokens.addAll(subTokens);
            for (String subToken : subTokens) {
                tokenIds.add(vocab.getOrDefault(subToken, vocab.get(UNK_TOKEN)));
            }
        }

        // Add [SEP] token at the end
        wordPieceTokens.add(SEP_TOKEN);
        tokenIds.add(vocab.get(SEP_TOKEN));

        // Truncate to max length
        int maxLength = modelConfig.getMaxLength();
        if (tokenIds.size() > maxLength) {
            tokenIds = tokenIds.subList(0, maxLength);
            // Ensure [SEP] is at the end
            tokenIds.set(maxLength - 1, vocab.get(SEP_TOKEN));
        }

        // Create attention mask
        int[] attentionMask = new int[tokenIds.size()];
        Arrays.fill(attentionMask, 1);

        // Convert to arrays
        int[] tokenIdsArray = tokenIds.stream().mapToInt(Integer::intValue).toArray();

        TokenizationResultDto tokenizationResultDto = new TokenizationResultDto();
        tokenizationResultDto.setTokenIds(tokenIdsArray);
        tokenizationResultDto.setAttentionMask(attentionMask);
        tokenizationResultDto.setTokens(wordPieceTokens);
        tokenizationResultDto.setOriginalText(text);
        tokenizationResultDto.setProcessedText(processedText);
        tokenizationResultDto.setTruncated(tokenIds.size() > maxLength);
        return tokenizationResultDto;
    }

    private String preprocessText(String text, boolean forRetrieval) {
        if (forRetrieval) {
            // Add retrieval instruction prefix
            text = "Represent this sentence for searching relevant passages: " + text;
        }

        // Normalize Unicode
        text = Normalizer.normalize(text, Normalizer.Form.NFKC);

        // Clean unwanted characters
        text = CLEAN_PATTERN.matcher(text).replaceAll("");

        // Handle Chinese characters - add spaces around them for proper tokenization
        text = addSpacesAroundChinese(text);

        return text.trim();
    }

    /**
     * Add spaces around Chinese characters for proper tokenization
     */
    private String addSpacesAroundChinese(String text) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (CHINESE_CHAR_PATTERN.matcher(String.valueOf(c)).matches()) {
                if (i > 0 && text.charAt(i - 1) != ' ') {
                    result.append(' ');
                }
                result.append(c);
                if (i < text.length() - 1 && text.charAt(i + 1) != ' ') {
                    result.append(' ');
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Basic tokenization (similar to BERT)
     */
    private List<String> basicTokenize(String text) {
        List<String> tokens = new ArrayList<>();

        // Split on whitespace
        String[] words = WHITESPACE_PATTERN.split(text);

        for (String word : words) {
            if (word.isEmpty()) continue;

            // Handle punctuation and special characters
            if (word.length() > 1) {
                tokens.addAll(splitPunctuation(word));
            } else {
                tokens.add(word);
            }
        }

        return tokens;
    }

    /**
     * Split punctuation from words (BERT-style)
     */
    private List<String> splitPunctuation(String word) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (PUNCTUATION_PATTERN.matcher(String.valueOf(c)).matches()) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                tokens.add(String.valueOf(c));
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        return tokens;
    }

    /**
     * WordPiece tokenization algorithm
     */
    private List<String> wordPieceTokenize(Map<String, Integer> vocab, String token) {
        if (vocab.containsKey(token)) {
            return Collections.singletonList(token);
        }

        List<String> subTokens = new ArrayList<>();
        int start = 0;
        int end = token.length();

        while (start < token.length()) {
            // Find the longest substring that is in the vocabulary
            String currentSubstring = null;
            end = token.length();

            while (start < end) {
                String substring = (start == 0) ? token.substring(start, end) : "##" + token.substring(start, end);
                if (vocab.containsKey(substring)) {
                    currentSubstring = substring;
                    break;
                }
                end--;
            }

            if (currentSubstring == null) {
                // No substring found, use UNK token
                subTokens.add(UNK_TOKEN);
                break;
            } else {
                subTokens.add(currentSubstring);
                start = end;
            }
        }

        return subTokens;
    }

    public Map<String, Object> getTokenizerConfig(String version) {
        return tokenizerDtos.get(version).getConfig();
    }
}
