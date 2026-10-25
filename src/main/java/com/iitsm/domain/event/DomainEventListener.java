package com.iitsm.domain.event;

/**
 * Pattern: Observer.
 * Consumer interface reacting to domain lifecycle events.
 */
@FunctionalInterface
public interface DomainEventListener {

    /**
     * Handles the published domain event.
     *
     * @param event strongly-typed domain event
     */
    void onEvent(DomainEvent event);
}

