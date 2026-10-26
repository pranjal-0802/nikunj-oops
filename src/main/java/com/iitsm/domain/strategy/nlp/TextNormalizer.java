package com.iitsm.domain.strategy.nlp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * OOP: Natural Language Processing text normalizer and tokenization utility.
 * Normalizes case, removes punctuation, filters stopwords, and applies algorithmic suffix stemming.
 */
public final class TextNormalizer {

    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("[^a-zA-Z0-9\\s]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are",
            "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but",
            "by", "can", "cannot", "could", "did", "do", "does", "doing", "down", "during", "each",
            "few", "for", "from", "further", "had", "has", "have", "having", "he", "her", "here",
            "hers", "herself", "him", "himself", "his", "how", "i", "if", "in", "into", "is", "it",
            "its", "itself", "just", "me", "more", "most", "my", "myself", "no", "nor", "not", "now",
            "of", "off", "on", "once", "only", "or", "other", "ought", "our", "ours", "ourselves",
            "out", "over", "own", "same", "she", "should", "so", "some", "such", "than", "that", "the",
            "their", "theirs", "them", "themselves", "then", "there", "these", "they", "this", "those",
            "through", "to", "too", "under", "until", "up", "very", "was", "we", "were", "what",
            "when", "where", "which", "while", "who", "whom", "why", "with", "would", "you", "your",
            "yours", "yourself", "yourselves"
    );

    private TextNormalizer() {
        // Utility class
    }

    /**
     * Normalizes an input string by lowercasing and replacing punctuation with whitespace.
     */
    public static String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String cleaned = PUNCTUATION_PATTERN.matcher(text.toLowerCase(Locale.ROOT)).replaceAll(" ");
        return WHITESPACE_PATTERN.matcher(cleaned).replaceAll(" ").trim();
    }

    /**
     * Tokenizes normalized text, removes stopwords, and stems words.
     */
    public static List<String> tokenize(String text) {
        String normalized = normalize(text);
        if (normalized.isEmpty()) {
            return Collections.emptyList();
        }

        String[] tokens = normalized.split(" ");
        List<String> result = new ArrayList<>(tokens.length);

        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.length() >= 2 && !STOP_WORDS.contains(trimmed)) {
                result.add(stem(trimmed));
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Lightweight algorithmic English stemmer normalizing common inflections (-ing, -ed, -es, -s, -ly, -ment).
     */
    public static String stem(String word) {
        if (word == null || word.length() <= 3) {
            return word;
        }

        String stemmed = word;

        if (stemmed.endsWith("sses")) {
            stemmed = stemmed.substring(0, stemmed.length() - 2);
        } else if (stemmed.endsWith("ies")) {
            stemmed = stemmed.substring(0, stemmed.length() - 3);
        } else if (stemmed.endsWith("ing") && stemmed.length() > 5) {
            stemmed = stemmed.substring(0, stemmed.length() - 3);
        } else if (stemmed.endsWith("ed") && stemmed.length() > 4) {
            stemmed = stemmed.substring(0, stemmed.length() - 2);
        } else if (stemmed.endsWith("ly") && stemmed.length() > 4) {
            stemmed = stemmed.substring(0, stemmed.length() - 2);
        } else if (stemmed.endsWith("ment") && stemmed.length() > 6) {
            stemmed = stemmed.substring(0, stemmed.length() - 4);
        } else if (stemmed.endsWith("ion") && stemmed.length() > 5) {
            stemmed = stemmed.substring(0, stemmed.length() - 3);
        } else if (stemmed.endsWith("es") && stemmed.length() > 4) {
            stemmed = stemmed.substring(0, stemmed.length() - 2);
        } else if (stemmed.endsWith("s") && !stemmed.endsWith("ss") && stemmed.length() > 3) {
            stemmed = stemmed.substring(0, stemmed.length() - 1);
        } else if (stemmed.endsWith("e") && !stemmed.endsWith("ee") && stemmed.length() > 4) {
            stemmed = stemmed.substring(0, stemmed.length() - 1);
        }

        // Clean double consonants (e.g. dropp -> drop)
        if (stemmed.length() > 3 && stemmed.charAt(stemmed.length() - 1) == stemmed.charAt(stemmed.length() - 2)) {
            char c = stemmed.charAt(stemmed.length() - 1);
            if (c == 'p' || c == 't' || c == 'b' || c == 'd' || c == 'g' || c == 'm' || c == 'n') {
                stemmed = stemmed.substring(0, stemmed.length() - 1);
            }
        }

        return stemmed;
    }
}

