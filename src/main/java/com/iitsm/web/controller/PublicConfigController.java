package com.iitsm.web.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public configuration endpoint for client applications.
 * Allows client UI to determine if demo mode is enabled without requiring authentication.
 */
@RestController
@RequestMapping("/api/public")
public class PublicConfigController {

    private final boolean demoMode;
    private final String defaultDomain;

    public PublicConfigController(
            @Value("${iitsm.demo-mode:true}") boolean demoMode,
            @Value("${iitsm.demo.domain:nimbusfinserve.com}") String defaultDomain) {
        this.demoMode = demoMode;
        this.defaultDomain = defaultDomain;
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getPublicConfig() {
        return ResponseEntity.ok(Map.of(
                "demoMode", demoMode,
                "defaultDomain", defaultDomain,
                "appName", "I-ITSM :: Nimbus FinServe",
                "version", "1.0.0"
        ));
    }
}
