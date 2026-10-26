package com.iitsm.application.service;

import com.iitsm.domain.model.EscalationRecord;
import com.iitsm.domain.model.SlaStatus;
import java.util.Optional;

/**
 * Value Object summarizing the outcome of an SLA evaluation on an incident.
 */
public record SlaEvaluationResult(
        String incidentId,
        SlaStatus previousStatus,
        SlaStatus currentStatus,
        double riskScore,
        boolean statusChanged,
        Optional<EscalationRecord> escalationTriggered) {}

