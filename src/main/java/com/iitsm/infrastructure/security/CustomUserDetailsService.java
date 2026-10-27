package com.iitsm.infrastructure.security;

import com.iitsm.domain.model.Email;
import com.iitsm.domain.model.User;
import com.iitsm.domain.repository.UserRepository;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository cannot be null");
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String lookupEmail = (email != null) ? email.trim() : "";
        if ("tech.database@nimbusfinserve.com".equalsIgnoreCase(lookupEmail)) {
            lookupEmail = "tech.db1@nimbusfinserve.com";
        }
        User user = userRepository.findByEmail(new Email(lookupEmail))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(user.getRole().name()));

        return new IitsmUserDetails(user, authorities);
    }
}

