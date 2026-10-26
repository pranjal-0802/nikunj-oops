package com.iitsm.domain.strategy.similarity;

/**
 * Pattern: Strategy.
 * Interface defining text similarity metrics for duplicate detection and correlation.
 */
public interface SimilarityMeasure {

    /**
     * Calculates the similarity score between two texts in the range [0.0, 1.0].
     */
    double calculateSimilarity(String textA, String textB);

    /**
     * Metric name.
     */
    String getMeasureName();
}

