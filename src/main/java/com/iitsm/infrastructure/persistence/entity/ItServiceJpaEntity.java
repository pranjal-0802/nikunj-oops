package com.iitsm.infrastructure.persistence.entity;

import com.iitsm.domain.model.BusinessCriticality;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "it_services")
public class ItServiceJpaEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "name", nullable = false, unique = true, length = 128)
    private String name;

    @Column(name = "owning_team_id", length = 64)
    private String owningTeamId;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_criticality", nullable = false, length = 32)
    private BusinessCriticality businessCriticality;

    @Column(name = "user_base", nullable = false)
    private int userBase;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "it_service_keywords", joinColumns = @JoinColumn(name = "service_id"))
    @Column(name = "keyword", length = 64)
    private Set<String> keywords = new HashSet<>();

    public ItServiceJpaEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOwningTeamId() { return owningTeamId; }
    public void setOwningTeamId(String owningTeamId) { this.owningTeamId = owningTeamId; }

    public BusinessCriticality getBusinessCriticality() { return businessCriticality; }
    public void setBusinessCriticality(BusinessCriticality businessCriticality) { this.businessCriticality = businessCriticality; }

    public int getUserBase() { return userBase; }
    public void setUserBase(int userBase) { this.userBase = userBase; }

    public Set<String> getKeywords() { return keywords; }
    public void setKeywords(Set<String> keywords) { this.keywords = keywords; }
}

