package com.iitsm.domain.strategy.classification;

import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.ItService;
import com.iitsm.domain.strategy.nlp.TextNormalizer;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * OOP: Matches free-text ticket content to registered IT Services based on name and keyword triggers.
 */
public class ServiceMatcher {

    public ServiceMatcher() {
    }

    /**
     * Resolves the most appropriate IT Service for an incident based on text and predicted category.
     */
    public Optional<ItService> findBestMatch(String title, String description, Collection<ItService> availableServices) {
        if (availableServices == null || availableServices.isEmpty()) {
            return Optional.empty();
        }

        String fullText = ((title != null ? title : "") + " " + (description != null ? description : "")).toLowerCase(Locale.ROOT);
        List<String> tokens = TextNormalizer.tokenize(fullText);

        ItService bestService = null;
        int maxMatches = 0;

        for (ItService service : availableServices) {
            int matches = 0;

            // Direct service name match
            if (fullText.contains(service.getName().toLowerCase(Locale.ROOT))) {
                matches += 3;
            }

            // Keyword tag matches
            for (String keyword : service.getKeywords()) {
                String normalizedKw = keyword.toLowerCase(Locale.ROOT);
                if (fullText.contains(normalizedKw)) {
                    matches += 2;
                } else {
                    for (String token : tokens) {
                        if (token.equalsIgnoreCase(normalizedKw)
                                || TextNormalizer.stem(normalizedKw).equalsIgnoreCase(token)) {
                            matches += 1;
                        }
                    }
                }
            }

            if (matches > maxMatches) {
                maxMatches = matches;
                bestService = service;
            }
        }

        if (maxMatches > 0 && bestService != null) {
            return Optional.of(bestService);
        }

        // Return empty when no keyword or service name matches
        return Optional.empty();
    }
}

