package com.iitsm.domain.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Pattern: Observer.
 * In-memory implementation of DomainEventPublisher.
 * Distributes events synchronously or asynchronously to registered DomainEventListeners.
 */
public class InMemoryDomainEventPublisher implements DomainEventPublisher {

    // CopyOnWriteArrayList chosen for thread-safe iteration without lock contention during event dispatch
    private final List<DomainEventListener> listeners = new CopyOnWriteArrayList<>();

    // Audit buffer of published events for verification and testing
    private final List<DomainEvent> publishedHistory = new CopyOnWriteArrayList<>();

    @Override
    public void publish(DomainEvent event) {
        Objects.requireNonNull(event, "event cannot be null");
        publishedHistory.add(event);
        for (DomainEventListener listener : listeners) {
            listener.onEvent(event);
        }
    }

    @Override
    public void registerListener(DomainEventListener listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        listeners.add(listener);
    }

    public List<DomainEvent> getPublishedHistory() {
        return Collections.unmodifiableList(publishedHistory);
    }

    public void clearHistory() {
        publishedHistory.clear();
    }
}

