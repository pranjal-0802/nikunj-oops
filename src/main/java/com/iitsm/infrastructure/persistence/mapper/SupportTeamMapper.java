package com.iitsm.infrastructure.persistence.mapper;

import com.iitsm.domain.model.SupportTeam;
import com.iitsm.infrastructure.persistence.entity.SupportTeamJpaEntity;
import java.util.HashSet;

public final class SupportTeamMapper {

    private SupportTeamMapper() {}

    public static SupportTeam toDomain(SupportTeamJpaEntity entity) {
        if (entity == null) return null;
        SupportTeam team = new SupportTeam(
                entity.getId(),
                entity.getName(),
                entity.getHandledCategories(),
                entity.getTeamLeadId());
        if (entity.getMemberIds() != null) {
            entity.getMemberIds().forEach(team::addMember);
        }
        return team;
    }

    public static SupportTeamJpaEntity toJpaEntity(SupportTeam domain) {
        if (domain == null) return null;
        SupportTeamJpaEntity entity = new SupportTeamJpaEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setTeamLeadId(domain.getTeamLeadId());
        entity.setHandledCategories(new HashSet<>(domain.getHandledCategories()));
        entity.setMemberIds(new HashSet<>(domain.getMemberIds()));
        return entity;
    }
}
