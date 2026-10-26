package com.iitsm.domain.strategy.nlp;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * OOP: Pure Java Term Frequency - Inverse Document Frequency (TF-IDF) Vectorizer.
 * Maps unstructured natural language text to sparse mathematical feature vectors and computes cosine similarity.
 */
public class TfIdfVectorizer {

    private final Map<String, Double> idfMap = new HashMap<>();
    private int totalDocuments = 0;

    public TfIdfVectorizer() {
    }

    /**
     * Trains the vectorizer on a corpus of documents to build IDF weights.
     */
    public synchronized void train(Collection<String> documents) {
        Objects.requireNonNull(documents, "documents cannot be null");
        idfMap.clear();
        totalDocuments = documents.size();

        if (totalDocuments == 0) {
            return;
        }

        Map<String, Integer> docFreq = new HashMap<>();
        for (String doc : documents) {
            List<String> tokens = TextNormalizer.tokenize(doc);
            Set<String> uniqueTokens = new HashSet<>(tokens);
            for (String token : uniqueTokens) {
                docFreq.merge(token, 1, Integer::sum);
            }
        }

        for (Map.Entry<String, Integer> entry : docFreq.entrySet()) {
            // Standard smooth IDF: ln(1 + (|D| / (1 + df)))
            double idf = Math.log(1.0 + ((double) totalDocuments / (1.0 + entry.getValue())));
            idfMap.put(entry.getKey(), idf);
        }
    }

    /**
     * Transforms a document string into a normalized TF-IDF vector.
     */
    public Map<String, Double> transform(String text) {
        List<String> tokens = TextNormalizer.tokenize(text);
        if (tokens.isEmpty()) {
            return Collections.emptyMap();
        }

        // 1. Compute Term Frequency (TF)
        Map<String, Double> tfMap = new HashMap<>();
        for (String token : tokens) {
            tfMap.merge(token, 1.0, Double::sum);
        }

        double docLength = tokens.size();
        Map<String, Double> vector = new HashMap<>();
        double sumSquares = 0.0;

        // 2. Compute TF-IDF weights: TF * IDF
        for (Map.Entry<String, Double> entry : tfMap.entrySet()) {
            String term = entry.getKey();
            double tf = entry.getValue() / docLength;
            // Default IDF for unseen out-of-vocabulary terms
            double idf = idfMap.getOrDefault(term, 1.0);
            double tfidf = tf * idf;
            vector.put(term, tfidf);
            sumSquares += tfidf * tfidf;
        }

        // 3. L2 Euclidean Vector Normalization
        if (sumSquares > 0.0) {
            double norm = Math.sqrt(sumSquares);
            for (Map.Entry<String, Double> entry : vector.entrySet()) {
                vector.put(entry.getKey(), entry.getValue() / norm);
            }
        }

        return Collections.unmodifiableMap(vector);
    }

    /**
     * Computes the Cosine Similarity between two TF-IDF vectors: (u . v) / (||u|| * ||v||).
     * Since transform() already normalizes vectors to unit length, this is the dot product.
     */
    public double cosineSimilarity(Map<String, Double> vectorA, Map<String, Double> vectorB) {
        if (vectorA == null || vectorB == null || vectorA.isEmpty() || vectorB.isEmpty()) {
            return 0.0;
        }

        Map<String, Double> smaller = vectorA.size() < vectorB.size() ? vectorA : vectorB;
        Map<String, Double> larger = vectorA.size() < vectorB.size() ? vectorB : vectorA;

        double dotProduct = 0.0;
        for (Map.Entry<String, Double> entry : smaller.entrySet()) {
            Double weightB = larger.get(entry.getKey());
            if (weightB != null) {
                dotProduct += entry.getValue() * weightB;
            }
        }
        return Math.max(0.0, Math.min(1.0, dotProduct));
    }

    public int getVocabularySize() {
        return idfMap.size();
    }
}

