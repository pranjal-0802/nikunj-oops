package com.iitsm.infrastructure.persistence.entity;

import com.iitsm.domain.model.Priority;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sla_policies")
public class SlaPolicyJpaEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 16)
    private Priority priority;

    @Column(name = "service_id_override", length = 64)
    private String serviceIdOverride;

    @Column(name = "response_target_seconds", nullable = false)
    private long responseTargetSeconds;

    @Column(name = "resolution_target_seconds", nullable = false)
    private long resolutionTargetSeconds;

    @Column(name = "warning_threshold_ratio", nullable = false)
    private double warningThresholdRatio;

    public SlaPolicyJpaEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public String getServiceIdOverride() { return serviceIdOverride; }
    public void setServiceIdOverride(String serviceIdOverride) { this.serviceIdOverride = serviceIdOverride; }

    public long getResponseTargetSeconds() { return responseTargetSeconds; }
    public void setResponseTargetSeconds(long responseTargetSeconds) { this.responseTargetSeconds = responseTargetSeconds; }

    public long getResolutionTargetSeconds() { return resolutionTargetSeconds; }
    public void setResolutionTargetSeconds(long resolutionTargetSeconds) { this.resolutionTargetSeconds = resolutionTargetSeconds; }

    public double getWarningThresholdRatio() { return warningThresholdRatio; }
    public void setWarningThresholdRatio(double warningThresholdRatio) { this.warningThresholdRatio = warningThresholdRatio; }
}

