package com.iitsm.application.port;

import com.iitsm.domain.model.User;

/**
 * Port for authentication token issuance and validation.
 */
public interface TokenPort {
    String generateToken(User user);
    String getEmailFromToken(String token);
    boolean validateToken(String token);
}

