package ru.yandex.practicum.mybank.accounts.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import ru.yandex.practicum.mybank.common.security.JwtAuthorityConverter;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/accounts/me", "/api/accounts/recipients")
                        .hasAuthority("SCOPE_accounts.read")
                        .requestMatchers(HttpMethod.PUT, "/api/accounts/me")
                        .hasAuthority("SCOPE_accounts.write")
                        .requestMatchers("/api/internal/**")
                        .hasAuthority("SCOPE_accounts.write")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(JwtAuthorityConverter.withRealmRoles())))
                .build();
    }
}
