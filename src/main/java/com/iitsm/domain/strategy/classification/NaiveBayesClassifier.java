package com.iitsm.domain.strategy.classification;

import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.strategy.nlp.TextNormalizer;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Pattern: Strategy.
 * Multinomial Naive Bayes probabilistic text classifier implemented in pure Java with Laplace smoothing.
 */
public class NaiveBayesClassifier implements IncidentClassifier {

    private static final String CLASSIFIER_NAME = "NAIVE_BAYES_CLASSIFIER";
    private static final double LAPLACE_ALPHA = 1.0;

    public record TrainingSample(IncidentCategory category, String text) {}

    private final Map<IncidentCategory, Integer> categoryDocCounts = new EnumMap<>(IncidentCategory.class);
    private final Map<IncidentCategory, Map<String, Integer>> categoryWordCounts = new EnumMap<>(IncidentCategory.class);
    private final Map<IncidentCategory, Integer> categoryTotalWords = new EnumMap<>(IncidentCategory.class);
    private final Set<String> vocabulary = new HashSet<>();
    private int totalTrainingDocs = 0;
    private boolean trained = false;

    public NaiveBayesClassifier() {
        for (IncidentCategory cat : IncidentCategory.values()) {
            categoryDocCounts.put(cat, 0);
            categoryWordCounts.put(cat, new HashMap<>());
            categoryTotalWords.put(cat, 0);
        }
    }

    /**
     * Trains the model on an in-memory collection of training samples.
     */
    public synchronized void train(List<TrainingSample> samples) {
        Objects.requireNonNull(samples, "samples cannot be null");
        reset();

        for (TrainingSample sample : samples) {
            addSample(sample.category(), sample.text());
        }
        totalTrainingDocs = samples.size();
        trained = totalTrainingDocs > 0;
    }

    /**
     * Loads and trains the model from a CSV resource stream (format: category,title,description).
     */
    public synchronized void trainFromCsv(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "inputStream cannot be null");
        List<TrainingSample> samples = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                if (line.isBlank()) {
                    continue;
                }
                // Parse CSV line
                String[] parts = line.split(",", 3);
                if (parts.length >= 2) {
                    try {
                        IncidentCategory cat = IncidentCategory.valueOf(parts[0].trim().toUpperCase());
                        String text = (parts.length == 3) ? parts[1] + " " + parts[2] : parts[1];
                        samples.add(new TrainingSample(cat, text));
                    } catch (IllegalArgumentException ignored) {
                        // Skip unrecognized categories
                    }
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load training corpus from CSV", ex);
        }

        train(samples);
    }

    private void addSample(IncidentCategory category, String text) {
        categoryDocCounts.merge(category, 1, Integer::sum);
        List<String> tokens = TextNormalizer.tokenize(text);
        Map<String, Integer> wordMap = categoryWordCounts.get(category);

        for (String token : tokens) {
            vocabulary.add(token);
            wordMap.merge(token, 1, Integer::sum);
            categoryTotalWords.merge(category, 1, Integer::sum);
        }
    }

    private void reset() {
        vocabulary.clear();
        for (IncidentCategory cat : IncidentCategory.values()) {
            categoryDocCounts.put(cat, 0);
            categoryWordCounts.get(cat).clear();
            categoryTotalWords.put(cat, 0);
        }
        totalTrainingDocs = 0;
        trained = false;
    }

    @Override
    public ClassificationResult classify(String title, String description) {
        if (!trained) {
            return ClassificationResult.of(IncidentCategory.APPLICATION, 0.10, CLASSIFIER_NAME,
                    List.of("Model not trained yet; defaulted to APPLICATION"));
        }

        String fullText = (title != null ? title : "") + " " + (description != null ? description : "");
        List<String> tokens = TextNormalizer.tokenize(fullText);

        if (tokens.isEmpty()) {
            return ClassificationResult.of(IncidentCategory.APPLICATION, 0.20, CLASSIFIER_NAME,
                    List.of("Empty tokens after normalization; defaulted to APPLICATION"));
        }

        Map<IncidentCategory, Double> logPosteriors = new EnumMap<>(IncidentCategory.class);
        double maxLog = Double.NEGATIVE_INFINITY;
        int vocabSize = Math.max(1, vocabulary.size());

        for (IncidentCategory category : IncidentCategory.values()) {
            // Prior probability with Laplace smoothing: P(C)
            int docCount = categoryDocCounts.getOrDefault(category, 0);
            double prior = Math.log((double) (docCount + 1) / (totalTrainingDocs + IncidentCategory.values().length));

            // Likelihood: sum(ln P(w|C))
            double logLikelihood = 0.0;
            Map<String, Integer> wordMap = categoryWordCounts.get(category);
            int totalWords = categoryTotalWords.getOrDefault(category, 0);
            double denominator = totalWords + (LAPLACE_ALPHA * vocabSize);

            for (String token : tokens) {
                int count = wordMap.getOrDefault(token, 0);
                double pTokenGivenC = (count + LAPLACE_ALPHA) / denominator;
                logLikelihood += Math.log(pTokenGivenC);
            }

            double totalLog = prior + logLikelihood;
            logPosteriors.put(category, totalLog);
            if (totalLog > maxLog) {
                maxLog = totalLog;
            }
        }

        // Softmax normalization over log-posteriors to compute calibrated confidence
        double sumExp = 0.0;
        for (double val : logPosteriors.values()) {
            sumExp += Math.exp(val - maxLog);
        }

        IncidentCategory bestCategory = IncidentCategory.APPLICATION;
        double bestProbability = 0.0;

        for (Map.Entry<IncidentCategory, Double> entry : logPosteriors.entrySet()) {
            double prob = Math.exp(entry.getValue() - maxLog) / sumExp;
            if (prob > bestProbability) {
                bestProbability = prob;
                bestCategory = entry.getKey();
            }
        }

        List<String> rationale = List.of(
                String.format("Naive Bayes posterior probability: %.2f%% across %d recognized tokens",
                        bestProbability * 100.0, tokens.size())
        );

        return ClassificationResult.of(bestCategory, bestProbability, CLASSIFIER_NAME, rationale);
    }

    @Override
    public String getName() {
        return CLASSIFIER_NAME;
    }

    public boolean isTrained() {
        return trained;
    }

    public int getVocabularySize() {
        return vocabulary.size();
    }
}

