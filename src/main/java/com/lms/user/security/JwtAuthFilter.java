package com.lms.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    // OncePerRequestFilter = Spring guarantees this runs exactly once per request

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain    // the chain of remaining filters
    ) throws ServletException, IOException {

        // 1. get the Authorization header
        final String authHeader = request.getHeader("Authorization");

        // 2. if no token present, skip this filter — let Spring Security handle it
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. extract the token (remove "Bearer " prefix — 7 characters)
        final String jwt = authHeader.substring(7);

        // 4. extract email from token
        final String userEmail = jwtService.extractUsername(jwt);

        // 5. if email found and user not already authenticated
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 6. load user from database
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // 7. validate token
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // 8. create authentication object and put it in SecurityContext
                // this tells Spring "this request is authenticated"
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,              // no credentials needed — token already validated
                                userDetails.getAuthorities()
                        );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 9. continue to the next filter / controller
        filterChain.doFilter(request, response);
    }
}