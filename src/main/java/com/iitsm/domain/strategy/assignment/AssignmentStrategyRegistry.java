package com.iitsm.domain.strategy.assignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pattern: Registry / Strategy.
 * Thread-safe registry managing available technician assignment strategies and active routing policy.
 */
public class AssignmentStrategyRegistry {

    private final Map<String, AssignmentStrategy> strategies = new ConcurrentHashMap<>();
    private volatile String activeStrategyName;

    public AssignmentStrategyRegistry() {
        register(new WeightedCompositeStrategy());
        register(new SkillBasedStrategy());
        register(new LoadBasedStrategy());
        register(new ExperienceBasedStrategy());
        register(new RoundRobinStrategy());
        this.activeStrategyName = WeightedCompositeStrategy.STRATEGY_NAME;
    }

    public void register(AssignmentStrategy strategy) {
        Objects.requireNonNull(strategy, "strategy cannot be null");
        strategies.put(strategy.getStrategyName().toUpperCase(), strategy);
    }

    public AssignmentStrategy getActiveStrategy() {
        AssignmentStrategy strategy = strategies.get(activeStrategyName.toUpperCase());
        if (strategy == null) {
            return strategies.get(WeightedCompositeStrategy.STRATEGY_NAME);
        }
        return strategy;
    }

    public synchronized void setActiveStrategy(String strategyName) {
        if (strategyName == null || !strategies.containsKey(strategyName.trim().toUpperCase())) {
            throw new IllegalArgumentException("Unknown assignment strategy: " + strategyName);
        }
        this.activeStrategyName = strategyName.trim().toUpperCase();
    }

    public String getActiveStrategyName() {
        return activeStrategyName;
    }

    public List<AssignmentStrategy> getAllStrategies() {
        return Collections.unmodifiableList(new ArrayList<>(strategies.values()));
    }

    public boolean hasStrategy(String strategyName) {
        return strategyName != null && strategies.containsKey(strategyName.trim().toUpperCase());
    }
}

