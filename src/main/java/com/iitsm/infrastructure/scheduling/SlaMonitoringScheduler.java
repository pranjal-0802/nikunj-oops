package com.iitsm.infrastructure.scheduling;

import com.iitsm.application.service.SlaService;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background daemon periodically checking SLA deadlines and triggering automated escalations.
 */
@Component
public class SlaMonitoringScheduler {

    private static final Logger log = LoggerFactory.getLogger(SlaMonitoringScheduler.class);

    private final SlaService slaService;

    public SlaMonitoringScheduler(SlaService slaService) {
        this.slaService = Objects.requireNonNull(slaService, "slaService cannot be null");
    }

    @Scheduled(fixedDelayString = "${iitsm.sla.scan-interval-ms:60000}")
    public void scanActiveSlas() {
        try {
            int evaluated = slaService.evaluateAllActiveIncidents().size();
            log.debug("SLA background scan completed: {} active incidents evaluated", evaluated);
        } catch (Exception ex) {
            log.error("Error during scheduled SLA background scan: {}", ex.getMessage(), ex);
        }
    }
}

