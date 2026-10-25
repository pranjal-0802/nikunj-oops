package com.iitsm.domain.exception;

import com.iitsm.domain.model.Priority;

/**
 * OOP: Thrown when no applicable SLA policy can be found for an incident.
 */
public class MissingSlaPolicyException extends IitsmException {

    public MissingSlaPolicyException(Priority priority, String serviceName) {
        super(
                "ERR_MISSING_SLA_POLICY",
                String.format(
                        "No SLA policy defined for priority '%s' and service '%s'",
                        priority, (serviceName != null ? serviceName : "GLOBAL")));
    }
}

