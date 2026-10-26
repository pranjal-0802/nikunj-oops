package com.iitsm.domain.strategy.nlp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for pure-Java TfIdfVectorizer and cosine similarity.
 */
class TfIdfVectorizerTest {

    @Test
    @DisplayName("Trains IDF weights and transforms text into normalized feature vectors")
    void testTrainAndTransform() {
        TfIdfVectorizer vectorizer = new TfIdfVectorizer();
        List<String> corpus = List.of(
                "Oracle database deadlock on transaction table",
                "PostgreSQL connection pool exhausted by database clients",
                "Laptop screen flickering and hardware display failure",
                "Cisco network switch port flapping and packet loss"
        );

        vectorizer.train(corpus);
        assertThat(vectorizer.getVocabularySize()).isGreaterThan(10);

        Map<String, Double> vec = vectorizer.transform("PostgreSQL database deadlock");
        assertThat(vec).isNotEmpty();
        assertThat(vec).containsKey("databas");

        // L2 unit length norm
        double sumSquares = vec.values().stream().mapToDouble(v -> v * v).sum();
        assertThat(sumSquares).isCloseTo(1.0, within(0.01));
    }

    @Test
    @DisplayName("Identical texts have cosine similarity of 1.0; unrelated have near 0.0")
    void testCosineSimilarity() {
        TfIdfVectorizer vectorizer = new TfIdfVectorizer();
        vectorizer.train(List.of(
                "AnyConnect VPN gateway timeout and dropped packets",
                "Laptop battery overheating and swelling chassis",
                "Employee benefits payroll portal HTTP 500 error"
        ));

        String textA = "VPN connection failed with gateway timeout";
        String textB = "VPN client cannot connect to gateway timeout error";
        String textC = "Laptop battery is overheating and melting plastic";

        Map<String, Double> vecA = vectorizer.transform(textA);
        Map<String, Double> vecB = vectorizer.transform(textB);
        Map<String, Double> vecC = vectorizer.transform(textC);

        double simAB = vectorizer.cosineSimilarity(vecA, vecB);
        double simAC = vectorizer.cosineSimilarity(vecA, vecC);

        assertThat(simAB).isGreaterThan(0.70);
        assertThat(simAC).isLessThan(0.20);
    }
}

