package ai.youthmind.filter;

import ai.youthmind.entity.User;
import ai.youthmind.service.RedisService;
import ai.youthmind.service.UserService;
import ai.youthmind.util.AuthenticationUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class RateFilter extends OncePerRequestFilter {

    private final RedisService redisService;

    private final UserService userService;

    // Default values for rate limits
    @Value("${rate.limit.anonymous.default:100}")
    private int anonymousDefaultThreshold;

    @Value("${rate.limit.authenticated.default:10}")
    private int authenticatedDefaultThreshold;

    @Value("${rate.limit.anonymous.sensitive:10}")
    private int anonymousSensitiveThreshold;

    @Value("${rate.limit.authenticated.sensitive:5}")
    private int authenticatedSensitiveThreshold;

    // Endpoint patterns
    private final Map<Pattern, String> endpointPatterns = new HashMap<>();

    public RateFilter(RedisService redisService, UserService userService) {
        this.redisService = redisService;
        this.userService = userService;

        // Define endpoint patterns and their associated rate limit types
        endpointPatterns.put(Pattern.compile("^/v1/auth/.*"), "sensitive");
        endpointPatterns.put(Pattern.compile("^/admin/.*"), "sensitive");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String path = request.getRequestURI();

        // Determine the rate limit type based on the request path
        String rateLimitType = "default";
        for (Map.Entry<Pattern, String> entry : endpointPatterns.entrySet()) {
            if (entry.getKey().matcher(path).matches()) {
                rateLimitType = entry.getValue();
                break;
            }
        }

        // Apply rate limiting based on authentication status and endpoint type
        if (auth == null || auth.getPrincipal() == null) {
            // Anonymous user
            String identifier = request.getRemoteAddr() + ":" + rateLimitType;
            int threshold = getThreshold(false, rateLimitType);

            RateLimitInfo rateLimitInfo = checkAndUpdateRateLimit(identifier, threshold, 300);
            setRateLimitHeaders(response, rateLimitInfo);

            if (rateLimitInfo.exceeded) {
                response.setStatus(429);
                return;
            }
        } else {
            boolean isAdmin = false;
            UUID userId = AuthenticationUtils.getCurrentUserIdOrNull();
            if (userId != null) {
                Optional<User> userOpt = userService.getUserById(userId);
                if (userOpt.isPresent()) {
                    isAdmin = userOpt.get().getRole() == User.Role.ADMIN;
                }
            }

            if (!isAdmin && request.getMethod().equals("POST")) {
                String identifier = auth.getPrincipal().toString() + ":" + rateLimitType;
                int threshold = getThreshold(true, rateLimitType);

                RateLimitInfo rateLimitInfo = checkAndUpdateRateLimit(identifier, threshold, 3600);
                setRateLimitHeaders(response, rateLimitInfo);

                if (rateLimitInfo.exceeded) {
                    response.setStatus(429);
                    return;
                }
            }

        }

        filterChain.doFilter(request, response);
    }

    private int getThreshold(boolean authenticated, String rateLimitType) {
        if (!authenticated) {
            return "sensitive".equals(rateLimitType) ?
                    anonymousSensitiveThreshold : anonymousDefaultThreshold;
        } else {
            return "sensitive".equals(rateLimitType) ?
                    authenticatedSensitiveThreshold : authenticatedDefaultThreshold;
        }
    }

    private RateLimitInfo checkAndUpdateRateLimit(String identifier, int threshold, int expiration) {
        Object count = redisService.getRateLimit(identifier);
        RateLimitInfo info = new RateLimitInfo();
        info.limit = threshold;
        info.remaining = threshold;

        // Get TTL from Redis if key exists
        Long ttl = redisService.getRateLimitTTL(identifier);
        if (ttl != null && ttl > 0) {
            info.reset = System.currentTimeMillis() / 1000 + ttl;
        } else {
            info.reset = System.currentTimeMillis() / 1000 + expiration;
        }

        if (count instanceof Integer countInt && countInt > 0) {
            info.remaining = Math.max(0, threshold - countInt);

            if (countInt < threshold) {
                redisService.incrementRateLimit(identifier);
                info.exceeded = false;
            } else {
                info.exceeded = true;
            }
        } else {
            redisService.setRateLimit(identifier, 1, expiration);
            info.remaining = threshold - 1;
            info.exceeded = false;
        }

        return info;
    }

    private void setRateLimitHeaders(HttpServletResponse response, RateLimitInfo info) {
        response.setHeader("X-RateLimit-Limit", String.valueOf(info.limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(info.remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(info.reset));

        if (info.exceeded) {
            response.setHeader("Retry-After", String.valueOf(info.reset - (System.currentTimeMillis() / 1000)));
        }
    }

    // Helper class to store rate limit information
    private static class RateLimitInfo {
        int limit;
        int remaining;
        long reset;
        boolean exceeded = false;
    }
}