package com.iitsm.infrastructure.security;

import com.iitsm.domain.model.User;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

/**
 * Custom Spring Security UserDetails carrying the domain User's tokenVersion and internal ID.
 * Enables immediate token revocation on tokenVersion bumps (logout, lock, password change).
 */
public class IitsmUserDetails extends org.springframework.security.core.userdetails.User {

    private final String userId;
    private final int tokenVersion;

    public IitsmUserDetails(User user, Collection<? extends GrantedAuthority> authorities) {
        super(
                user.getEmail().value(),
                user.getPasswordHash(),
                user.isActive(),
                true,
                true,
                !user.isLocked(),
                authorities
        );
        this.userId = user.getId();
        this.tokenVersion = user.getTokenVersion();
    }

    public String getUserId() {
        return userId;
    }

    public int getTokenVersion() {
        return tokenVersion;
    }
}

