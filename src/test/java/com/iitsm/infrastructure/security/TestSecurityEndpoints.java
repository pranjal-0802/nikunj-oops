package com.iitsm.infrastructure.security;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestSecurityEndpoints {

    @GetMapping("/employee/test")
    public ResponseEntity<String> employeeOnly() {
        return ResponseEntity.ok("Hello Employee");
    }

    @GetMapping("/admin/test")
    public ResponseEntity<String> adminOnly() {
        return ResponseEntity.ok("Hello Admin");
    }
}

