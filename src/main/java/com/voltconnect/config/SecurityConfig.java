package com.voltconnect.config;

import com.voltconnect.auth.JwtAuthenticationFilter;
import com.voltconnect.shared.dto.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Instant;

/**
 * Spring Security configuration for the Volt-Connect backend.
 *
 * <p>Security rules:
 * <ul>
 *   <li>All requests to {@code /api/v1/auth/**} are permitted without authentication
 *       (OTP verification, token refresh).</li>
 *   <li>Swagger UI and API docs are permitted in all profiles (restrict in prod via env if needed).</li>
 *   <li>All other routes require a valid JWT in the {@code Authorization: Bearer} header.</li>
 *   <li>CSRF is disabled — the API is stateless and uses JWT, not cookies.</li>
 *   <li>Session management is STATELESS — no HTTP session is created or used.</li>
 * </ul>
 *
 * <p>The actual JWT validation is performed by {@code JwtAuthenticationFilter} (Task 4),
 * which is registered here as a pre-filter. The placeholder below will be replaced once
 * that filter is implemented.
 *
 * <p>Satisfies Requirements 1.2 and 20.1.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Public endpoints that do not require authentication.
     */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api-docs/**",
            "/actuator/health",
            "/actuator/info"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ── Disable CSRF — stateless REST API uses JWT, not cookies ──────────
                .csrf(AbstractHttpConfigurer::disable)

                // ── Stateless session — no HttpSession created or used ────────────────
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ── Authorization rules ───────────────────────────────────────────────
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated()
                )

                // ── Custom 401 response (JSON, not redirect) ──────────────────────────
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            ApiErrorResponse error = ApiErrorResponse.of(
                                    HttpStatus.UNAUTHORIZED.value(),
                                    "Unauthorized",
                                    "Authentication required — provide a valid Bearer token",
                                    request.getRequestURI());
                            response.getWriter().write(objectMapper.writeValueAsString(error));
                        })
                        // ── Custom 403 response ───────────────────────────────────────
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            ApiErrorResponse error = ApiErrorResponse.of(
                                    HttpStatus.FORBIDDEN.value(),
                                    "Forbidden",
                                    "You do not have permission to access this resource",
                                    request.getRequestURI());
                            response.getWriter().write(objectMapper.writeValueAsString(error));
                        })
                );

        // ── JWT filter — validates Bearer token before Spring Security auth ─────
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
