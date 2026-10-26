package com.iitsm.simulation;

import java.util.List;

public record SimulationResult(
        String scenarioId,
        String scenarioName,
        boolean success,
        String summary,
        List<String> stepLogs,
        String primaryIncidentId) {}

