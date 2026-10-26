package com.iitsm.domain.chain.escalation;

import java.time.Instant;
import java.util.Objects;

/**
 * OOP: Context object supplied to the Escalation Chain of Responsibility.
 */
public record EscalationContext(
        Instant currentInstant,
        String supportTeamLeadId,
        String systemAdminId,
        String itManagerId,
        boolean isMissionCriticalService) {

    public EscalationContext {
        Objects.requireNonNull(currentInstant, "currentInstant cannot be null");
        Objects.requireNonNull(systemAdminId, "systemAdminId cannot be null");
        Objects.requireNonNull(itManagerId, "itManagerId cannot be null");
    }
}

