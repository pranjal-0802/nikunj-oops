package com.iitsm.web.controller;

import com.iitsm.simulation.SimulationResult;
import com.iitsm.simulation.SimulationService;
import com.iitsm.simulation.SimulationStatusDto;
import java.time.Duration;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller providing administrative controls for the time-accelerated simulation engine
 * and demonstration scenario execution (S1–S5).
 */
@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = Objects.requireNonNull(simulationService, "simulationService cannot be null");
    }

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SimulationStatusDto> getStatus() {
        return ResponseEntity.ok(simulationService.getClockStatus());
    }

    @PostMapping("/speed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SimulationStatusDto> setSpeed(@RequestParam(name = "multiplier", defaultValue = "1") long multiplier) {
        simulationService.setSpeedMultiplier(multiplier);
        return ResponseEntity.ok(simulationService.getClockStatus());
    }

    @PostMapping("/advance")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SimulationStatusDto> advanceTime(@RequestParam(name = "minutes", defaultValue = "15") long minutes) {
        simulationService.advanceClock(Duration.ofMinutes(minutes));
        return ResponseEntity.ok(simulationService.getClockStatus());
    }

    @PostMapping("/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SimulationStatusDto> resetClock() {
        simulationService.resetClock();
        return ResponseEntity.ok(simulationService.getClockStatus());
    }

    @PostMapping("/run/{scenarioId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SimulationResult> runScenario(@PathVariable("scenarioId") String scenarioId) {
        SimulationResult result = simulationService.runScenario(scenarioId);
        return ResponseEntity.ok(result);
    }
}

