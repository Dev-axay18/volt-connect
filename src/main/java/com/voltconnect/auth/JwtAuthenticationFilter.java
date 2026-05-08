package com.voltconnect.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Servlet filter that validates the JWT Bearer token on every incoming request.
 *
 * <p>Behaviour:
 * <ul>
 *   <li>Skips public endpoints ({@code /api/v1/auth/**}).</li>
 *   <li>Reads the {@code Authorization: Bearer <token>} header.</li>
 *   <li>If the token is valid and is an access token, populates the
 *       {@link SecurityContextHolder} with the user's id and roles.</li>
 *   <li>If the token is missing or invalid, the filter does <em>not</em> throw —
 *       it simply lets the request continue. Spring Security's
 *       {@code AuthenticationEntryPoint} will return 401 for protected routes.</li>
 * </ul>
 *
 * <p>Satisfies Requirements 1.2, 20.1, 20.2.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX         = "Bearer ";

    private static final String[] PUBLIC_PATHS = {
            "/api/v1/auth/**"
    };

    private final JwtTokenProvider jwtTokenProvider;
    private final AntPathMatcher   pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        for (String pattern : PUBLIC_PATHS) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractBearerToken(request);

        if (token != null
                && jwtTokenProvider.validateToken(token)
                && jwtTokenProvider.isAccessToken(token)) {

            String       userId = jwtTokenProvider.extractUserId(token);
            List<String> roles  = jwtTokenProvider.extractRoles(token);

            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .toList();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Authenticated user '{}' with roles {}", userId, roles);
        }

        filterChain.doFilter(request, response);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
