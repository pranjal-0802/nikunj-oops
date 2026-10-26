package com.iitsm.domain.strategy.classification;

/**
 * Pattern: Strategy.
 * Interface defining extensible incident classification algorithms.
 */
public interface IncidentClassifier {

    /**
     * Identifies the probable incident category based on title and description.
     */
    ClassificationResult classify(String title, String description);

    /**
     * Unique identifier for this classification strategy.
     */
    String getName();
}

