package com.iitsm.domain.strategy.assignment;

import com.iitsm.domain.exception.TechnicianUnavailableException;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.Skill;
import com.iitsm.domain.model.Technician;
import com.iitsm.domain.strategy.nlp.TextNormalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Pattern: Strategy.
 * Routes incidents to the technician with highest technical skill overlap against category and keywords.
 */
public class SkillBasedStrategy implements AssignmentStrategy {

    public static final String STRATEGY_NAME = "SKILL_BASED";

    @Override
    public AssignmentRecommendation recommend(Incident incident, List<Technician> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            throw new TechnicianUnavailableException("No available technicians found for skill-based assignment");
        }

        String fullText = ((incident.getTitle() != null ? incident.getTitle() : "") + " "
                + (incident.getDescription() != null ? incident.getDescription() : "")).toLowerCase(Locale.ROOT);
        List<String> tokens = TextNormalizer.tokenize(fullText);

        Technician bestTech = null;
        double bestScore = -1.0;
        String bestRationale = "";

        for (Technician tech : candidates) {
            int categoryMatchCount = 0;
            int keywordMatchCount = 0;

            for (Skill skill : tech.getSkills()) {
                if (incident.getCategory() != null && skill.category() == incident.getCategory()) {
                    categoryMatchCount++;
                }
                String skillName = skill.name().toLowerCase(Locale.ROOT);
                if (fullText.contains(skillName)) {
                    keywordMatchCount += 2;
                } else {
                    for (String token : tokens) {
                        if (skillName.contains(token)) {
                            keywordMatchCount += 1;
                        }
                    }
                }
            }

            // Raw skill score normalized to [0.0, 1.0]
            double skillScore = Math.min(1.0, (categoryMatchCount * 0.30) + (keywordMatchCount * 0.15));
            // Slight load tiebreak (prefer less loaded if skill is similar)
            double loadPenalty = tech.getLoadRatio() * 0.10;
            double effectiveScore = Math.max(0.0, skillScore - loadPenalty);

            if (effectiveScore > bestScore) {
                bestScore = effectiveScore;
                bestTech = tech;
                bestRationale = String.format("Selected for high skill alignment: %d category skills, %d keyword matches (score: %.2f)",
                        categoryMatchCount, keywordMatchCount, skillScore);
            }
        }

        if (bestTech == null) {
            bestTech = candidates.get(0);
            bestRationale = "Default fallback candidate from eligible pool";
            bestScore = 0.50;
        }

        return new AssignmentRecommendation(bestTech, bestScore, STRATEGY_NAME, bestRationale);
    }

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    @Override
    public String getDescription() {
        return "Routes incidents to technicians whose skill profile best matches the incident category and keywords.";
    }
}

