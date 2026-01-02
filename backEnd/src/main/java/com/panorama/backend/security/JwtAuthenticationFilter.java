package com.panorama.backend.security;

import com.panorama.backend.service.auth.UserAccountDetailsService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserAccountDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        final String jwt = authHeader.substring(7);
        try {
            Claims claims = jwtService.extractAllClaims(jwt);
            String username = claims.getSubject();
            if (StringUtils.hasText(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
                    Object rolesClaim = claims.get("roles");
                    if (rolesClaim instanceof List<?> roles) {
                        List<GrantedAuthority> claimAuthorities = roles.stream()
                                .map(Object::toString)
                                .map(String::trim)
                                .filter(role -> !role.isEmpty())
                                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                                .distinct()
                                .map(SimpleGrantedAuthority::new)
                                .map(GrantedAuthority.class::cast)
                                .toList();
                        if (!claimAuthorities.isEmpty()) {
                            authorities = claimAuthorities;
                        }
                    } else if (rolesClaim instanceof String roleText && !roleText.isBlank()) {
                        String role = roleText.trim();
                        String normalized = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                        authorities = List.of(new SimpleGrantedAuthority(normalized));
                    }

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }
}
