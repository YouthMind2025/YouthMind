package ai.youthmind.filter;

import ai.youthmind.service.RedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class RateFilter extends OncePerRequestFilter {

    private RedisService redisService;

    public RateFilter(RedisService redisService) {
        this.redisService = redisService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getPrincipal() == null) {
            String key = "youthmind:rate:" + request.getRemoteAddr();
            Object count = redisService.getValue(key);
            if (count instanceof Integer countInt && countInt > 0) {
                if (countInt < 5) {
                    redisService.increment(key);
                } else {
                    response.setStatus(429);
                    return;
                }
            } else {
                redisService.setValue(key, 1, 30, TimeUnit.SECONDS);
            }
        } else if (request.getMethod().equals("POST")) {
            String key = "youthmind:rate:" + auth.getPrincipal().toString();
            Object count = redisService.getValue(key);
            if (count instanceof Integer countInt && countInt > 0) {
                if (countInt < 10) {
                    redisService.increment(key);
                } else {
                    response.setStatus(429);
                    return;
                }
            } else {
                redisService.setValue(key, 1, 3600, TimeUnit.SECONDS);
            }
        }

        filterChain.doFilter(request, response);
    }

}
