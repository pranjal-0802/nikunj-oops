package com.iitsm.web.controller;

import com.iitsm.application.service.UserService;
import com.iitsm.domain.model.Designation;
import com.iitsm.domain.model.Employee;
import com.iitsm.domain.model.User;
import com.iitsm.web.dto.AuthResponse;
import com.iitsm.web.dto.LoginRequest;
import com.iitsm.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller managing authentication, token issuance, and employee self-registration.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = Objects.requireNonNull(userService, "userService cannot be null");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        UserService.AuthResult result = userService.authenticate(request.email(), request.password());
        User user = result.user();
        return ResponseEntity.ok(new AuthResponse(
                result.token(),
                user.getId(),
                user.getName(),
                user.getEmail().value(),
                user.getRole().name()));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        // Finding S3: Self-registration ignores client-supplied designation to prevent privilege escalation
        Designation designation = Designation.ASSOCIATE;

        String dept = (request.department() != null && !request.department().isBlank())
                ? request.department().trim()
                : "Engineering";

        Employee registered = userService.registerEmployee(
                request.name(),
                request.email(),
                request.password(),
                designation,
                dept,
                1);

        UserService.AuthResult result = userService.authenticate(request.email(), request.password());
        return ResponseEntity.ok(new AuthResponse(
                result.token(),
                registered.getId(),
                registered.getName(),
                registered.getEmail().value(),
                registered.getRole().name()));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        String email = authentication.getName();
        User user = userService.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).build();
        }
        return ResponseEntity.ok(new AuthResponse(
                null,
                user.getId(),
                user.getName(),
                user.getEmail().value(),
                user.getRole().name()));
    }
}
