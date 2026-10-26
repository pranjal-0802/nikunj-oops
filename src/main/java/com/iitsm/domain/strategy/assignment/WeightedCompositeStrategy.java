package com.iitsm.domain.strategy.assignment;

import com.iitsm.domain.exception.TechnicianUnavailableException;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.Skill;
import com.iitsm.domain.model.Technician;
import com.iitsm.domain.strategy.nlp.TextNormalizer;
import java.util.List;
import java.util.Locale;

/**
 * Pattern: Composite / Strategy.
 * Default intelligent routing strategy combining skills (0.50), load (0.30), and experience (0.20).
 * Dynamically tunes factor weights based on incident priority.
 */
public class WeightedCompositeStrategy implements AssignmentStrategy {

    public static final String STRATEGY_NAME = "WEIGHTED_COMPOSITE";

    @Override
    public AssignmentRecommendation recommend(Incident incident, List<Technician> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            throw new TechnicianUnavailableException("No available technicians found for weighted composite assignment");
        }

        Priority priority = (incident.getPriorityDecision() != null)
                ? incident.getPriorityDecision().priority()
                : Priority.P3;

        // Dynamic weight tuning based on incident priority
        double skillWeight;
        double loadWeight;
        double expWeight;

        if (priority == Priority.P1 || priority == Priority.P2) {
            // For critical incidents, prioritize skills and experience over pure load
            skillWeight = 0.45;
            expWeight = 0.35;
            loadWeight = 0.20;
        } else if (priority == Priority.P4) {
            // For low urgency incidents, prioritize load balancing
            loadWeight = 0.50;
            skillWeight = 0.30;
            expWeight = 0.20;
        } else {
            // Default baseline
            skillWeight = 0.50;
            loadWeight = 0.30;
            expWeight = 0.20;
        }

        String fullText = ((incident.getTitle() != null ? incident.getTitle() : "") + " "
                + (incident.getDescription() != null ? incident.getDescription() : "")).toLowerCase(Locale.ROOT);
        List<String> tokens = TextNormalizer.tokenize(fullText);

        Technician bestCandidate = null;
        double bestCompositeScore = -1.0;
        String bestRationale = "";

        for (Technician tech : candidates) {
            // 1. Skill Score [0.0, 1.0]
            int skillMatches = 0;
            for (Skill skill : tech.getSkills()) {
                if (incident.getCategory() != null && skill.category() == incident.getCategory()) {
                    skillMatches += 2;
                }
                String skillName = skill.name().toLowerCase(Locale.ROOT);
                if (fullText.contains(skillName)) {
                    skillMatches += 2;
                } else {
                    for (String token : tokens) {
                        if (skillName.contains(token)) {
                            skillMatches += 1;
                        }
                    }
                }
            }
            double skillScore = Math.min(1.0, skillMatches * 0.20);

            // 2. Load Score [0.0, 1.0] (1.0 = completely free, 0.0 = full)
            double loadScore = Math.max(0.0, 1.0 - tech.getLoadRatio());

            // 3. Experience Score [0.0, 1.0]
            double expScore = Math.min(1.0, tech.getExperienceYears() / 12.0);

            // Composite Weighted Sum
            double composite = (skillScore * skillWeight) + (loadScore * loadWeight) + (expScore * expWeight);

            if (composite > bestCompositeScore) {
                bestCompositeScore = composite;
                bestCandidate = tech;
                bestRationale = String.format("Composite score: %.2f (Skills: %.2f * %.2f, Load: %.2f * %.2f, Exp: %.2f * %.2f)",
                        composite, skillScore, skillWeight, loadScore, loadWeight, expScore, expWeight);
            }
        }

        if (bestCandidate == null) {
            bestCandidate = candidates.get(0);
            bestCompositeScore = 0.50;
            bestRationale = "Default fallback candidate from available roster";
        }

        return new AssignmentRecommendation(bestCandidate, bestCompositeScore, STRATEGY_NAME, bestRationale);
    }

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    @Override
    public String getDescription() {
        return "Intelligent multi-factor routing balancing skills, current capacity, and experience, dynamically tuned by incident priority.";
    }
}

