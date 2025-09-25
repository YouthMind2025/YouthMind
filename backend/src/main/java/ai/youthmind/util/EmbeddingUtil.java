package ai.youthmind.util;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.Arrays;

public class EmbeddingUtil {

    public static double cosineSimilarity(double[] embedding1, double[] embedding2) {
        if (embedding1.length != embedding2.length) {
            throw new IllegalArgumentException("Embeddings must have same dimension");
        }

        RealVector v1 = new ArrayRealVector(embedding1);
        RealVector v2 = new ArrayRealVector(embedding2);

        double dotProduct = v1.dotProduct(v2);
        double norm1 = v1.getNorm();
        double norm2 = v2.getNorm();

        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }

        return dotProduct / (norm1 * norm2);
    }

    public static double euclideanDistance(double[] embedding1, double[] embedding2) {
        RealVector v1 = new ArrayRealVector(embedding1);
        RealVector v2 = new ArrayRealVector(embedding2);
        return v1.getDistance(v2);
    }

    public static double calculateNorm(double[] embedding) {
        RealVector vector = new ArrayRealVector(embedding);
        return vector.getNorm();
    }

    public static double calculateZeroRatio(double[] embedding) {
        long zeroCount = Arrays.stream(embedding).filter(v -> v == 0.0f).count();
        return (double) zeroCount / embedding.length;
    }

    public static double calculateNanRatio(double[] embedding) {
        long nanCount = Arrays.stream(embedding).filter(Double::isNaN).count();
        return (double) nanCount / embedding.length;
    }

    public static DescriptiveStatistics calculateStatistics(double[] embedding) {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        Arrays.stream(embedding).forEach(stats::addValue);
        return stats;
    }

    public static double calculateVariance(double[] embedding) {
        DescriptiveStatistics stats = calculateStatistics(embedding);
        return stats.getVariance();
    }

    public static boolean isNormalized(double[] embedding, double tolerance) {
        double norm = calculateNorm(embedding);
        return Math.abs(norm - 1.0) <= tolerance;
    }

}
