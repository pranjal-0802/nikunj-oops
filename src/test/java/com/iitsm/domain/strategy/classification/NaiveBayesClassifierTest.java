package com.iitsm.domain.strategy.classification;

import static org.assertj.core.api.Assertions.assertThat;

import com.iitsm.domain.model.IncidentCategory;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit and benchmark tests for NaiveBayesClassifier and CompositeClassifier.
 */
class NaiveBayesClassifierTest {

    private static NaiveBayesClassifier nbClassifier;
    private static KeywordClassifier keywordClassifier;
    private static CompositeClassifier compositeClassifier;

    @BeforeAll
    static void setUp() {
        nbClassifier = new NaiveBayesClassifier();
        try (InputStream is = NaiveBayesClassifierTest.class.getClassLoader().getResourceAsStream("sim/corpus.csv")) {
            assertThat(is).isNotNull();
            nbClassifier.trainFromCsv(is);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load corpus", ex);
        }

        keywordClassifier = new KeywordClassifier();
        compositeClassifier = new CompositeClassifier(keywordClassifier, nbClassifier);
    }

    @Test
    @DisplayName("Model trains successfully with vocabulary size >= 100")
    void testModelTrainingMetrics() {
        assertThat(nbClassifier.isTrained()).isTrue();
        assertThat(nbClassifier.getVocabularySize()).isGreaterThanOrEqualTo(100);
    }

    @Test
    @DisplayName("Accurately classifies distinct unseen benchmark tickets with >= 80% accuracy")
    void testEvaluationBenchmarkAccuracy() throws Exception {
        int total = 0;
        int correct = 0;

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("sim/test_tickets.csv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                if (line.isBlank()) continue;

                String[] parts = line.split(",", 3);
                if (parts.length >= 3) {
                    IncidentCategory expected = IncidentCategory.valueOf(parts[0].trim());
                    String title = parts[1];
                    String desc = parts[2];

                    ClassificationResult result = compositeClassifier.classify(title, desc);
                    total++;
                    if (result.category() == expected) {
                        correct++;
                    }
                }
            }
        }

        double accuracy = (double) correct / (double) total;
        System.out.printf("Composite Classifier Benchmark Accuracy: %d/%d (%.2f%%)%n", correct, total, accuracy * 100.0);
        assertThat(accuracy).isGreaterThanOrEqualTo(0.80);
    }

    @Test
    @DisplayName("Handles out-of-vocabulary terms gracefully via Laplace smoothing")
    void testLaplaceSmoothingOnUnseenTerms() {
        ClassificationResult result = nbClassifier.classify(
                "Xyzw9999 totallyunheardofword qwertyuiop1234",
                "Asdfghjk zxcvbnm completely foreign text without domain tokens"
        );
        assertThat(result).isNotNull();
        assertThat(result.category()).isNotNull();
        assertThat(result.confidence()).isBetween(0.0, 1.0);
    }

    @Test
    @DisplayName("KeywordClassifier matches high-confidence keywords and triggers triage for ambiguous text")
    void testKeywordClassifierAndTriageThreshold() {
        ClassificationResult strongResult = keywordClassifier.classify(
                "Core network switch port flapping and VPN gateway timeout",
                "AnyConnect client dropped connection with packet loss"
        );
        assertThat(strongResult.category()).isEqualTo(IncidentCategory.NETWORK);
        assertThat(strongResult.confidence()).isGreaterThanOrEqualTo(0.75);
        assertThat(strongResult.requiresTriage()).isFalse();

        ClassificationResult ambiguousResult = keywordClassifier.classify(
                "Something went wrong today",
                "Please check this as soon as possible"
        );
        assertThat(ambiguousResult.requiresTriage()).isTrue();
    }
}

