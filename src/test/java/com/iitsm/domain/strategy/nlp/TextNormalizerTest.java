package com.iitsm.domain.strategy.nlp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for TextNormalizer (tokenization, stopword removal, and algorithmic stemming).
 */
class TextNormalizerTest {

    @Test
    @DisplayName("Normalizes text: lowercases and strips punctuation")
    void testNormalize() {
        String input = "Hello, World! This is a TEST: 123... VPN-Gateway down???";
        String normalized = TextNormalizer.normalize(input);
        assertThat(normalized).isEqualTo("hello world this is a test 123 vpn gateway down");
    }

    @Test
    @DisplayName("Handles null and blank inputs gracefully")
    void testNullAndBlankInput() {
        assertThat(TextNormalizer.normalize(null)).isEmpty();
        assertThat(TextNormalizer.normalize("   ")).isEmpty();
        assertThat(TextNormalizer.tokenize(null)).isEmpty();
        assertThat(TextNormalizer.tokenize("   ")).isEmpty();
    }

    @Test
    @DisplayName("Tokenizes and filters common English stopwords")
    void testStopwordRemoval() {
        String input = "The database connection was dropped by the server and we could not reconnect";
        List<String> tokens = TextNormalizer.tokenize(input);

        // 'the', 'was', 'by', 'and', 'we', 'could', 'not' should be removed
        assertThat(tokens).contains("databas", "connect", "drop", "server", "reconnect");
        assertThat(tokens).doesNotContain("the", "was", "by", "and", "we");
    }

    @Test
    @DisplayName("Stems common English inflections correctly")
    void testStemming() {
        assertThat(TextNormalizer.stem("crashing")).isEqualTo("crash");
        assertThat(TextNormalizer.stem("crashed")).isEqualTo("crash");
        assertThat(TextNormalizer.stem("processes")).isEqualTo("process");
        assertThat(TextNormalizer.stem("queries")).isEqualTo("quer");
        assertThat(TextNormalizer.stem("slowly")).isEqualTo("slow");
        assertThat(TextNormalizer.stem("department")).isEqualTo("depart");
        assertThat(TextNormalizer.stem("servers")).isEqualTo("server");
    }
}

