package io.github.virolac.orders.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRealmRoleConverterTest {
    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

    @Test
    void realmRolesBecomePrefixedAuthorities() {
        Jwt jwt = jwtWithClaim("realm_access", Map.of("roles", List.of("customer", "admin")));

        assertThat(converter.convert(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_customer", "ROLE_admin");
    }

    @Test
    void missingRealmAccessGivesNoAuthorities() {
        Jwt jwt = jwtWithClaim("sub", "alice");

        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    void realmAccessWithoutRolesGivesNoAuthorities() {
        Jwt jwt = jwtWithClaim("realm_access", Map.of("other", "value"));

        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    void nonStringRolesAreIgnored() {
        Jwt jwt = jwtWithClaim("realm_access", Map.of("roles", List.of("customer", 42)));

        assertThat(converter.convert(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_customer");
    }

    private static Jwt jwtWithClaim(String name, Object value) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim(name, value)
                .build();
    }
}
