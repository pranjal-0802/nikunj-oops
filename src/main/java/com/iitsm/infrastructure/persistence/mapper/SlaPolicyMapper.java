package com.iitsm.infrastructure.persistence.mapper;

import com.iitsm.domain.model.SlaPolicy;
import com.iitsm.infrastructure.persistence.entity.SlaPolicyJpaEntity;
import java.time.Duration;

public final class SlaPolicyMapper {

    private SlaPolicyMapper() {}

    public static SlaPolicy toDomain(SlaPolicyJpaEntity entity) {
        if (entity == null) return null;
        return new SlaPolicy(
                entity.getId(),
                entity.getPriority(),
                entity.getServiceIdOverride(),
                Duration.ofSeconds(entity.getResponseTargetSeconds()),
                Duration.ofSeconds(entity.getResolutionTargetSeconds()),
                entity.getWarningThresholdRatio());
    }

    public static SlaPolicyJpaEntity toJpaEntity(SlaPolicy domain) {
        if (domain == null) return null;
        SlaPolicyJpaEntity entity = new SlaPolicyJpaEntity();
        entity.setId(domain.getId());
        entity.setPriority(domain.getPriority());
        entity.setServiceIdOverride(domain.getServiceIdOverride());
        entity.setResponseTargetSeconds(domain.getResponseTarget().toSeconds());
        entity.setResolutionTargetSeconds(domain.getResolutionTarget().toSeconds());
        entity.setWarningThresholdRatio(domain.getWarningThresholdRatio());
        return entity;
    }
}

