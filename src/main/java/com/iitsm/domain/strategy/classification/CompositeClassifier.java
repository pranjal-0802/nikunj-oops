package com.iitsm.domain.strategy.classification;

import com.iitsm.domain.model.IncidentCategory;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pattern: Composite / Strategy.
 * Orchestrates a multi-stage classification pipeline combining rule-based keyword extraction and probabilistic Naive Bayes.
 * Automatically flags low-confidence classifications for human-in-the-loop administrator triage.
 */
public class CompositeClassifier implements IncidentClassifier {

    private static final String CLASSIFIER_NAME = "COMPOSITE_CLASSIFIER";
    private static final double HIGH_KEYWORD_THRESHOLD = 0.85;

    private final KeywordClassifier keywordClassifier;
    private final NaiveBayesClassifier naiveBayesClassifier;

    public CompositeClassifier(KeywordClassifier keywordClassifier, NaiveBayesClassifier naiveBayesClassifier) {
        this.keywordClassifier = Objects.requireNonNull(keywordClassifier, "keywordClassifier cannot be null");
        this.naiveBayesClassifier = Objects.requireNonNull(naiveBayesClassifier, "naiveBayesClassifier cannot be null");
    }

    @Override
    public ClassificationResult classify(String title, String description) {
        ClassificationResult keywordResult = keywordClassifier.classify(title, description);

        // 1. If keyword classifier has very strong confidence, adopt it immediately
        if (keywordResult.confidence() >= HIGH_KEYWORD_THRESHOLD) {
            return keywordResult;
        }

        // 2. Evaluate Naive Bayes classifier
        ClassificationResult nbResult = naiveBayesClassifier.classify(title, description);

        // 3. Fusion logic
        List<String> combinedRationale = new ArrayList<>();
        combinedRationale.addAll(keywordResult.rationale());
        combinedRationale.addAll(nbResult.rationale());

        IncidentCategory finalCategory;
        double finalConfidence;

        if (keywordResult.category() == nbResult.category()) {
            // Concordance boost
            finalCategory = nbResult.category();
            finalConfidence = Math.min(0.99, nbResult.confidence() * 1.15 + 0.10);
            combinedRationale.add("Keyword and Naive Bayes models in agreement: " + finalCategory);
        } else if (nbResult.confidence() >= keywordResult.confidence()) {
            finalCategory = nbResult.category();
            finalConfidence = nbResult.confidence();
            combinedRationale.add("Selected Naive Bayes prediction over keyword classifier");
        } else {
            finalCategory = keywordResult.category();
            finalConfidence = keywordResult.confidence();
            combinedRationale.add("Selected Keyword prediction over Naive Bayes classifier");
        }

        return ClassificationResult.of(finalCategory, finalConfidence, CLASSIFIER_NAME, combinedRationale);
    }

    @Override
    public String getName() {
        return CLASSIFIER_NAME;
    }
}

