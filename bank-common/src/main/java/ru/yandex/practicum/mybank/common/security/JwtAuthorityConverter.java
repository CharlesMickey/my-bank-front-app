package ru.yandex.practicum.mybank.common.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public final class JwtAuthorityConverter {
    private static final String PRINCIPAL_CLAIM = "preferred_username";

    private JwtAuthorityConverter() {
    }

    public static Converter<Jwt, AbstractAuthenticationToken> withRealmRoles() {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName(PRINCIPAL_CLAIM);
        converter.setJwtGrantedAuthoritiesConverter(jwt -> mergeAuthorities(scopes.convert(jwt), realmRoles(jwt)));
        return converter;
    }

    private static Collection<GrantedAuthority> mergeAuthorities(Collection<GrantedAuthority> scopes,
                                                                 Collection<GrantedAuthority> roles) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (scopes != null) {
            authorities.addAll(scopes);
        }
        authorities.addAll(roles);
        return authorities;
    }

    private static Collection<GrantedAuthority> realmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }

        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(role -> new SimpleGrantedAuthority("SCOPE_" + role))
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}
