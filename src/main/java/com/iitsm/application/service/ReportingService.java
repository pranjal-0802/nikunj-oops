package com.iitsm.application.service;

import com.iitsm.domain.model.EscalationRecord;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.IncidentStatus;
import com.iitsm.domain.model.ItService;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.SlaStatus;
import com.iitsm.domain.model.SlaTracker;
import com.iitsm.domain.model.SupportTeam;
import com.iitsm.domain.model.Technician;
import com.iitsm.domain.repository.IncidentRepository;
import com.iitsm.domain.repository.ItServiceRepository;
import com.iitsm.domain.repository.SupportTeamRepository;
import com.iitsm.domain.repository.TechnicianRepository;
import com.iitsm.web.dto.EscalationReportDto;
import com.iitsm.web.dto.MttrReportDto;
import com.iitsm.web.dto.ServiceAnalysisReportDto;
import com.iitsm.web.dto.SlaPerformanceReportDto;
import com.iitsm.web.dto.WorkloadReportDto;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service compiling management reports, SLA compliance analytics, and operational metrics.
 */
@Service
@Transactional(readOnly = true)
public class ReportingService {

    private final IncidentRepository incidentRepository;
    private final TechnicianRepository technicianRepository;
    private final SupportTeamRepository supportTeamRepository;
    private final ItServiceRepository itServiceRepository;
    private final EscalationService escalationService;

    public ReportingService(
            IncidentRepository incidentRepository,
            TechnicianRepository technicianRepository,
            SupportTeamRepository supportTeamRepository,
            ItServiceRepository itServiceRepository,
            EscalationService escalationService) {
        this.incidentRepository = Objects.requireNonNull(incidentRepository);
        this.technicianRepository = Objects.requireNonNull(technicianRepository);
        this.supportTeamRepository = Objects.requireNonNull(supportTeamRepository);
        this.itServiceRepository = Objects.requireNonNull(itServiceRepository);
        this.escalationService = Objects.requireNonNull(escalationService);
    }

    /**
     * Compiles workload and capacity utilization metrics across technicians and support teams.
     */
    public WorkloadReportDto generateWorkloadReport() {
        List<Technician> technicians = technicianRepository.findAll();
        List<Incident> allIncidents = incidentRepository.findAll();

        Map<String, Integer> resolvedCountMap = new HashMap<>();
        for (Incident inc : allIncidents) {
            if (inc.getAssigneeId() != null &&
                    (inc.getStatus() == IncidentStatus.RESOLVED || inc.getStatus() == IncidentStatus.VERIFIED || inc.getStatus() == IncidentStatus.CLOSED)) {
                resolvedCountMap.merge(inc.getAssigneeId(), 1, Integer::sum);
            }
        }

        List<WorkloadReportDto.TechnicianLoadItem> techItems = new ArrayList<>();
        double totalLoadRatio = 0.0;

        for (Technician tech : technicians) {
            int active = tech.getActiveIncidentCount();
            int maxCap = tech.getMaxCapacity();
            double loadRatio = (maxCap > 0) ? (double) active / maxCap : 0.0;
            totalLoadRatio += loadRatio;
            int resolved = resolvedCountMap.getOrDefault(tech.getId(), 0);

            techItems.add(new WorkloadReportDto.TechnicianLoadItem(
                    tech.getId(),
                    tech.getName(),
                    tech.getSupportTeamId(),
                    active,
                    maxCap,
                    Math.round(loadRatio * 100.0) / 100.0,
                    resolved
            ));
        }

        double avgLoadRatio = technicians.isEmpty() ? 0.0 : Math.round((totalLoadRatio / technicians.size()) * 100.0) / 100.0;

        // Team metrics
        List<SupportTeam> teams = supportTeamRepository.findAll();
        List<WorkloadReportDto.TeamLoadItem> teamItems = new ArrayList<>();

        for (SupportTeam team : teams) {
            List<Technician> teamMembers = technicians.stream()
                    .filter(t -> Objects.equals(t.getSupportTeamId(), team.getId()))
                    .toList();

            int teamActive = teamMembers.stream().mapToInt(Technician::getActiveIncidentCount).sum();
            int teamCap = teamMembers.stream().mapToInt(Technician::getMaxCapacity).sum();
            double teamLoadRatio = (teamCap > 0) ? (double) teamActive / teamCap : 0.0;

            teamItems.add(new WorkloadReportDto.TeamLoadItem(
                    team.getId(),
                    team.getName(),
                    teamMembers.size(),
                    teamActive,
                    teamCap,
                    Math.round(teamLoadRatio * 100.0) / 100.0
            ));
        }

        return new WorkloadReportDto(techItems, teamItems, avgLoadRatio);
    }

    /**
     * Analyzes incident distributions across IT services and categories.
     */
    public ServiceAnalysisReportDto generateServiceAnalysisReport() {
        List<ItService> services = itServiceRepository.findAll();
        List<Incident> allIncidents = incidentRepository.findAll();

        Map<String, Long> categoryDist = new HashMap<>();
        Map<String, List<Incident>> byService = new HashMap<>();

        for (Incident inc : allIncidents) {
            if (inc.getCategory() != null) {
                categoryDist.merge(inc.getCategory().name(), 1L, Long::sum);
            }
            if (inc.getServiceId() != null) {
                byService.computeIfAbsent(inc.getServiceId(), k -> new ArrayList<>()).add(inc);
            }
        }

        List<ServiceAnalysisReportDto.ServiceMetricItem> serviceMetrics = new ArrayList<>();
        for (ItService svc : services) {
            List<Incident> svcIncidents = byService.getOrDefault(svc.getId(), List.of());
            long total = svcIncidents.size();
            long active = svcIncidents.stream()
                    .filter(i -> i.getStatus() != IncidentStatus.VERIFIED && i.getStatus() != IncidentStatus.CLOSED)
                    .count();
            long p1Count = svcIncidents.stream()
                    .filter(i -> i.getPriority() == Priority.P1)
                    .count();

            serviceMetrics.add(new ServiceAnalysisReportDto.ServiceMetricItem(
                    svc.getId(),
                    svc.getName(),
                    svc.getOwningTeamId(),
                    svc.getBusinessCriticality().name(),
                    total,
                    active,
                    p1Count
            ));
        }

        return new ServiceAnalysisReportDto(serviceMetrics, categoryDist);
    }

    /**
     * Compiles SLA compliance percentages and breach counts overall and by priority.
     */
    public SlaPerformanceReportDto generateSlaPerformanceReport() {
        List<Incident> allIncidents = incidentRepository.findAll();

        long metCount = 0;
        long breachedCount = 0;
        long warningCount = 0;
        long atRiskCount = 0;

        Map<Priority, Long> metByPriority = new EnumMap<>(Priority.class);
        Map<Priority, Long> breachedByPriority = new EnumMap<>(Priority.class);

        for (Incident inc : allIncidents) {
            SlaTracker tracker = inc.getSlaTracker();
            if (tracker == null) {
                continue;
            }

            SlaStatus status = tracker.getStatus();
            Priority p = inc.getPriority();

            switch (status) {
                case MET -> {
                    metCount++;
                    if (p != null) metByPriority.merge(p, 1L, Long::sum);
                }
                case BREACHED -> {
                    breachedCount++;
                    if (p != null) breachedByPriority.merge(p, 1L, Long::sum);
                }
                case WARNING -> warningCount++;
                case AT_RISK -> atRiskCount++;
                case ON_TRACK -> {}
            }
        }

        long totalEvaluated = metCount + breachedCount;
        double overallCompliance = (totalEvaluated > 0)
                ? Math.round(((double) metCount / totalEvaluated) * 10000.0) / 100.0
                : 100.0;

        Map<String, SlaPerformanceReportDto.PrioritySlaMetric> priorityMetrics = new HashMap<>();
        for (Priority p : Priority.values()) {
            long met = metByPriority.getOrDefault(p, 0L);
            long breached = breachedByPriority.getOrDefault(p, 0L);
            long total = met + breached;
            double pct = (total > 0) ? Math.round(((double) met / total) * 10000.0) / 100.0 : 100.0;
            priorityMetrics.put(p.name(), new SlaPerformanceReportDto.PrioritySlaMetric(met, breached, pct));
        }

        return new SlaPerformanceReportDto(
                totalEvaluated,
                metCount,
                breachedCount,
                warningCount,
                atRiskCount,
                overallCompliance,
                priorityMetrics
        );
    }

    /**
     * Calculates Mean Time to Resolution (MTTR) and Mean Time to Respond (MTTResp).
     */
    public MttrReportDto generateMttrReport() {
        List<Incident> allIncidents = incidentRepository.findAll();

        List<Double> resolutionDurations = new ArrayList<>();
        List<Double> responseDurations = new ArrayList<>();

        Map<String, List<Double>> resolutionByCategory = new HashMap<>();
        Map<String, List<Double>> resolutionByPriority = new HashMap<>();

        for (Incident inc : allIncidents) {
            SlaTracker tracker = inc.getSlaTracker();
            if (tracker == null) continue;

            Instant createdAt = inc.getCreatedAt();
            Instant respondedAt = tracker.getRespondedAt();
            Instant resolvedAt = tracker.getResolvedAt();

            if (respondedAt != null && !respondedAt.isBefore(createdAt)) {
                double respMinutes = Duration.between(createdAt, respondedAt).toMinutes();
                responseDurations.add(respMinutes);
            }

            if (resolvedAt != null && !resolvedAt.isBefore(createdAt)) {
                double resMinutes = Duration.between(createdAt, resolvedAt).toMinutes();
                resolutionDurations.add(resMinutes);

                if (inc.getCategory() != null) {
                    resolutionByCategory.computeIfAbsent(inc.getCategory().name(), k -> new ArrayList<>())
                            .add(resMinutes);
                }
                if (inc.getPriority() != null) {
                    resolutionByPriority.computeIfAbsent(inc.getPriority().name(), k -> new ArrayList<>())
                            .add(resMinutes);
                }
            }
        }

        double overallMttr = calculateAverage(resolutionDurations);
        double overallMttresp = calculateAverage(responseDurations);

        Map<String, Double> byCatMap = new HashMap<>();
        for (Map.Entry<String, List<Double>> e : resolutionByCategory.entrySet()) {
            byCatMap.put(e.getKey(), calculateAverage(e.getValue()));
        }

        Map<String, Double> byPriMap = new HashMap<>();
        for (Map.Entry<String, List<Double>> e : resolutionByPriority.entrySet()) {
            byPriMap.put(e.getKey(), calculateAverage(e.getValue()));
        }

        return new MttrReportDto(overallMttr, overallMttresp, byCatMap, byPriMap);
    }

    /**
     * Compiles an audit report of all escalations triggered across the organisation.
     */
    public EscalationReportDto generateEscalationReport() {
        List<EscalationRecord> all = escalationService.getAllEscalations();
        long total = all.size();

        Map<String, Long> byTier = all.stream()
                .collect(Collectors.groupingBy(EscalationRecord::toRole, Collectors.counting()));

        List<EscalationRecord> recent = all.stream().limit(50).toList();

        return new EscalationReportDto(total, byTier, recent);
    }

    private double calculateAverage(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (double v : values) sum += v;
        return Math.round((sum / values.size()) * 10.0) / 10.0;
    }
}
