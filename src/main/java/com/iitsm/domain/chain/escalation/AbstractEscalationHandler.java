package com.iitsm.domain.chain.escalation;

import com.iitsm.domain.model.Incident;
import java.util.Optional;

/**
 * Pattern: Chain of Responsibility (Abstract Base Handler).
 * Handles boilerplate chaining and traversal logic for escalation handlers.
 */
public abstract class AbstractEscalationHandler implements EscalationHandler {

    protected EscalationHandler next;

    @Override
    public void setNext(EscalationHandler next) {
        this.next = next;
    }

    @Override
    public EscalationHandler getNext() {
        return next;
    }

    protected Optional<EscalationAction> forward(Incident incident, EscalationContext context) {
        if (next != null) {
            return next.handle(incident, context);
        }
        return Optional.empty();
    }
}

