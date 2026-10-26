package com.iitsm.domain.chain.escalation;

import java.util.Objects;

/**
 * OOP: Value Object encapsulating an escalation decision emitted by the Chain of Responsibility.
 */
public record EscalationAction(
        int tier,
        String tierName,
        String targetUserId,
        String targetRole,
        String reason) {

    public EscalationAction {
        Objects.requireNonNull(tierName, "tierName cannot be null");
        Objects.requireNonNull(targetUserId, "targetUserId cannot be null");
        Objects.requireNonNull(targetRole, "targetRole cannot be null");
        Objects.requireNonNull(reason, "reason cannot be null");
    }
}

