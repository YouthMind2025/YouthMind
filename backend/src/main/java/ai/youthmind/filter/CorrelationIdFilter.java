package ai.youthmind.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter to generate and propagate correlation IDs for request tracing.
 * 
 * This filter:
 * - Generates a unique correlation ID for each request if not provided
 * - Stores the correlation ID in SLF4J's MDC for logging context
 * - Adds the correlation ID to response headers for client propagation
 * - Ensures MDC is cleared after request processing
 * 
 * The correlation ID enables tracing user requests through the entire system,
 * allowing for end-to-end observability and debugging.
 */
@Component
@Order(1)
public class CorrelationIdFilter implements Filter {
    
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String MDC_CORRELATION_ID = "correlationId";
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        try {
            // Get or generate correlation ID
            String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = UUID.randomUUID().toString();
            }
            
            // Store in MDC for logging
            MDC.put(MDC_CORRELATION_ID, correlationId);
            
            // Add to response headers
            httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);
            
            chain.doFilter(request, response);
            
        } finally {
            MDC.clear();
        }
    }
}
