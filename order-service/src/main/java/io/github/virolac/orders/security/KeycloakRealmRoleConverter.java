package io.github.virolac.orders.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) {
            return List.of();
        }

        if (!(realmAccess.get("roles") instanceof List<?> roles)) {
            return List.of();
        }

        Collection<GrantedAuthority> authorities = new ArrayList<>();
        for (Object role : roles) {
            if (role instanceof String name) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + name));
            }
        }

        return authorities;
    }
}
