package com.iitsm.web.controller;

import com.iitsm.application.service.ReportingService;
import com.iitsm.web.dto.EscalationReportDto;
import com.iitsm.web.dto.MttrReportDto;
import com.iitsm.web.dto.ServiceAnalysisReportDto;
import com.iitsm.web.dto.SlaPerformanceReportDto;
import com.iitsm.web.dto.WorkloadReportDto;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing administrative operational reports and executive SLA compliance analytics.
 */
@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private final ReportingService reportingService;

    public ReportController(ReportingService reportingService) {
        this.reportingService = Objects.requireNonNull(reportingService, "reportingService cannot be null");
    }

    @GetMapping("/workload")
    public ResponseEntity<WorkloadReportDto> getWorkloadReport() {
        return ResponseEntity.ok(reportingService.generateWorkloadReport());
    }

    @GetMapping("/services")
    public ResponseEntity<ServiceAnalysisReportDto> getServiceAnalysisReport() {
        return ResponseEntity.ok(reportingService.generateServiceAnalysisReport());
    }

    @GetMapping("/sla")
    public ResponseEntity<SlaPerformanceReportDto> getSlaPerformanceReport() {
        return ResponseEntity.ok(reportingService.generateSlaPerformanceReport());
    }

    @GetMapping("/mttr")
    public ResponseEntity<MttrReportDto> getMttrReport() {
        return ResponseEntity.ok(reportingService.generateMttrReport());
    }

    @GetMapping("/escalations")
    public ResponseEntity<EscalationReportDto> getEscalationReport() {
        return ResponseEntity.ok(reportingService.generateEscalationReport());
    }
}

