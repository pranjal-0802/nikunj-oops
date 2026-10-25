package com.iitsm.web.dto;

public record AuthResponse(
        String token,
        String userId,
        String name,
        String email,
        String role) {}

