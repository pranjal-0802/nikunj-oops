package com.iitsm.infrastructure.persistence.entity;

import com.iitsm.domain.model.IncidentCategory;
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
@Table(name = "support_teams")
public class SupportTeamJpaEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "name", nullable = false, unique = true, length = 128)
    private String name;

    @Column(name = "team_lead_id", length = 64)
    private String teamLeadId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "support_team_categories", joinColumns = @JoinColumn(name = "support_team_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 32)
    private Set<IncidentCategory> handledCategories = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "support_team_members", joinColumns = @JoinColumn(name = "support_team_id"))
    @Column(name = "technician_id", length = 64)
    private Set<String> memberIds = new HashSet<>();

    public SupportTeamJpaEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTeamLeadId() { return teamLeadId; }
    public void setTeamLeadId(String teamLeadId) { this.teamLeadId = teamLeadId; }

    public Set<IncidentCategory> getHandledCategories() { return handledCategories; }
    public void setHandledCategories(Set<IncidentCategory> handledCategories) { this.handledCategories = handledCategories; }

    public Set<String> getMemberIds() { return memberIds; }
    public void setMemberIds(Set<String> memberIds) { this.memberIds = memberIds; }
}
