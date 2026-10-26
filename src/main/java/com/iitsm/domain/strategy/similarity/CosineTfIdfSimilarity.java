package com.iitsm.domain.strategy.similarity;

import com.iitsm.domain.strategy.nlp.TfIdfVectorizer;
import java.util.Map;
import java.util.Objects;

/**
 * Pattern: Strategy.
 * Calculates text similarity using TF-IDF vectorization and cosine dot product.
 */
public class CosineTfIdfSimilarity implements SimilarityMeasure {

    public static final String MEASURE_NAME = "COSINE_TF_IDF";

    private final TfIdfVectorizer vectorizer;

    public CosineTfIdfSimilarity(TfIdfVectorizer vectorizer) {
        this.vectorizer = Objects.requireNonNull(vectorizer, "vectorizer cannot be null");
    }

    @Override
    public double calculateSimilarity(String textA, String textB) {
        if (textA == null || textB == null || textA.isBlank() || textB.isBlank()) {
            return 0.0;
        }
        Map<String, Double> vecA = vectorizer.transform(textA);
        Map<String, Double> vecB = vectorizer.transform(textB);
        return vectorizer.cosineSimilarity(vecA, vecB);
    }

    @Override
    public String getMeasureName() {
        return MEASURE_NAME;
    }
}

