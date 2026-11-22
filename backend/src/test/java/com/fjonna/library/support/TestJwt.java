package com.fjonna.library.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Builds a mock JWT (via spring-security-test's {@code jwt()}) shaped like a real Keycloak token:
 * {@code sub}, {@code preferred_username}, {@code email} and {@code realm_access.roles}. Spring
 * Security authorities are derived from the same realm roles passed in, so the mock token stays
 * consistent with what {@code SecurityConfig}'s real JwtAuthenticationConverter would produce from
 * an identical real token.
 */
public final class TestJwt {

    private TestJwt() {
    }

    public static RequestPostProcessor user(UUID id, String username, String email, String... realmRoles) {
        List<GrantedAuthority> authorities = Arrays.stream(realmRoles)
            .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
            .toList();
        return jwt()
            .jwt(builder -> builder
                .subject(id.toString())
                .claim("preferred_username", username)
                .claim("email", email)
                .claim("realm_access", Map.of("roles", List.of(realmRoles))))
            .authorities(authorities);
    }

    /** Convenience: a plain realm USER. */
    public static RequestPostProcessor user(UUID id, String username, String email) {
        return user(id, username, email, "USER");
    }

    /** Convenience: a realm ADMIN (also carries USER, matching how admins are set up in the real realm). */
    public static RequestPostProcessor admin(UUID id, String username, String email) {
        return user(id, username, email, "USER", "ADMIN");
    }
}
