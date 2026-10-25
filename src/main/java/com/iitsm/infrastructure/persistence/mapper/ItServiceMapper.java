package com.iitsm.infrastructure.persistence.mapper;

import com.iitsm.domain.model.ItService;
import com.iitsm.infrastructure.persistence.entity.ItServiceJpaEntity;
import java.util.HashSet;

public final class ItServiceMapper {

    private ItServiceMapper() {}

    public static ItService toDomain(ItServiceJpaEntity entity) {
        if (entity == null) return null;
        return new ItService(
                entity.getId(),
                entity.getName(),
                entity.getOwningTeamId(),
                entity.getBusinessCriticality(),
                entity.getUserBase(),
                entity.getKeywords());
    }

    public static ItServiceJpaEntity toJpaEntity(ItService domain) {
        if (domain == null) return null;
        ItServiceJpaEntity entity = new ItServiceJpaEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setOwningTeamId(domain.getOwningTeamId());
        entity.setBusinessCriticality(domain.getBusinessCriticality());
        entity.setUserBase(domain.getUserBase());
        entity.setKeywords(new HashSet<>(domain.getKeywords()));
        return entity;
    }
}
