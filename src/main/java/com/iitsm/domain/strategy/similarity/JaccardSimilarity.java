package com.iitsm.domain.strategy.similarity;

import com.iitsm.domain.strategy.nlp.TextNormalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pattern: Strategy.
 * Computes Jaccard index (set intersection over union) on normalized token sets.
 */
public class JaccardSimilarity implements SimilarityMeasure {

    public static final String MEASURE_NAME = "JACCARD";

    @Override
    public double calculateSimilarity(String textA, String textB) {
        if (textA == null || textB == null || textA.isBlank() || textB.isBlank()) {
            return 0.0;
        }

        List<String> tokensA = TextNormalizer.tokenize(textA);
        List<String> tokensB = TextNormalizer.tokenize(textB);

        if (tokensA.isEmpty() || tokensB.isEmpty()) {
            return 0.0;
        }

        Set<String> setA = new HashSet<>(tokensA);
        Set<String> setB = new HashSet<>(tokensB);

        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);

        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size() / (double) union.size();
    }

    @Override
    public String getMeasureName() {
        return MEASURE_NAME;
    }
}

