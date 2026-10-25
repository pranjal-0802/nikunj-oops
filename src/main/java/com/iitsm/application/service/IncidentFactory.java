package com.iitsm.application.service;

import com.iitsm.domain.model.Incident;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Pattern: Factory.
 * Constructs new Incident aggregate roots, enforcing validation and temporal consistency via Clock.
 */
@Component
public class IncidentFactory {

    private final Clock clock;

    public IncidentFactory(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "Clock cannot be null");
    }

    public Incident createIncident(String title, String description, String reporterId) {
        String id = "INC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Instant now = clock.instant();
        return new Incident(id, title, description, reporterId, now);
    }
}

