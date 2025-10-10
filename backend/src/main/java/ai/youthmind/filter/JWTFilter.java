package ai.youthmind.filter;

import ai.youthmind.entity.User;
import ai.youthmind.service.JWTService;
import ai.youthmind.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JWTFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JWTFilter.class);

    private JWTService jwtService;

    private UserService userService;

    public JWTFilter(JWTService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String email = null;

        logger.debug("JWT Filter: Processing request to: {}", request.getRequestURI());
        logger.debug("JWT Filter: Authorization header: {}", authHeader != null ? "Present" : "Missing");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7).trim(); // Add trim() to remove any whitespace
            logger.debug("JWT Filter: Token extracted, length: {}", token.length());
            try {
                email = jwtService.extractEmail(token);
                logger.debug("JWT Filter: Extracted email: {}", email);
            } catch (Exception e) {
                // Log the error and continue with the filter chain
                logger.error("Invalid JWT token: {}", e.getMessage());
                filterChain.doFilter(request, response);
                return;
            }
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            User user = userService.getUserByEmail(email).orElse(null);
            logger.debug("JWT Filter: Found user: {}", user != null ? user.getEmail() : "null");
            if (user != null && jwtService.validateToken(token, user)) {
                // Extract role from token and create authorities
                String role = jwtService.extractRole(token);
                logger.debug("JWT Filter: User role: {}", role);
                List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "USER")));
                
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(user.getId(), null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                logger.debug("JWT Filter: Authentication set for user: {}", user.getEmail());
            } else {
                logger.debug("JWT Filter: Token validation failed or user not found");
            }
        } else {
            logger.debug("JWT Filter: Email is null or authentication already exists");
        }
        filterChain.doFilter(request, response);
    }

}
