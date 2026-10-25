package com.iitsm.domain.event;

/**
 * Pattern: Observer.
 * Port interface for publishing domain events across the system.
 */
public interface DomainEventPublisher {

    /**
     * Publishes a domain event to all subscribed listeners.
     */
    void publish(DomainEvent event);

    /**
     * Registers a domain event listener.
     */
    void registerListener(DomainEventListener listener);
}

