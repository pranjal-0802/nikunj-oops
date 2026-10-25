package com.iitsm.infrastructure.config;

import com.iitsm.domain.event.DomainEventListener;
import com.iitsm.domain.event.DomainEventPublisher;
import com.iitsm.domain.event.InMemoryDomainEventPublisher;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration wiring the event spine.
 * Automatically discovers all registered DomainEventListener beans (e.g. AuditTrailObserver, NotificationObserver)
 * and subscribes them to the singleton DomainEventPublisher.
 */
@Configuration
public class DomainEventConfig {

    @Bean
    public DomainEventPublisher domainEventPublisher(List<DomainEventListener> listeners) {
        InMemoryDomainEventPublisher publisher = new InMemoryDomainEventPublisher();
        for (DomainEventListener listener : listeners) {
            publisher.registerListener(listener);
        }
        return publisher;
    }
}

