package com.iitsm.simulation;

import java.time.Instant;

public record SimulationStatusDto(
        Instant virtualTime,
        long speedMultiplier,
        String formattedVirtualTime,
        boolean isAccelerated) {}

