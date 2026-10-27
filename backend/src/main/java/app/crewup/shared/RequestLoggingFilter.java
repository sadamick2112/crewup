package app.crewup.shared;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Writes one INFO line per {@code /api} request (M0-26): method, route pattern, status, duration and the caller's token
 * subject. It never reads query strings, headers or bodies, so no personal data or tokens reach the log (LOG-3).
 *
 * <p>It wraps the security filters so requests that security rejects (401) are logged too. Tracing runs further out,
 * so every line carries the request's trace ID.
 */
@Component
class RequestLoggingFilter extends OncePerRequestFilter implements Ordered {

    static final String UNMATCHED_ROUTE = "UNMATCHED";

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    // The bearer-token filter stores the authenticated context here; it's still readable after the chain returns,
    // when the security context holder has already been cleared.
    private final RequestAttributeSecurityContextRepository securityContexts =
            new RequestAttributeSecurityContextRepository();

    @Override
    public int getOrder() {
        return SecurityFilterProperties.DEFAULT_FILTER_ORDER - 1;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            LoggingEventBuilder line = log.atInfo()
                    .addKeyValue("http.method", request.getMethod())
                    .addKeyValue("http.route", route(request))
                    .addKeyValue("http.status", response.getStatus())
                    .addKeyValue("durationMs", durationMs);
            subject(request).ifPresent(subject -> line.addKeyValue("subject", subject));
            line.log("Request completed");
        }
    }

    /** The matched pattern (for example {@code /api/groups/{groupId}}), never the raw URL with its IDs. */
    private static String route(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern != null ? pattern.toString() : UNMATCHED_ROUTE;
    }

    private Optional<String> subject(HttpServletRequest request) {
        var authentication = securityContexts.loadDeferredContext(request).get().getAuthentication();
        return authentication instanceof JwtAuthenticationToken token
                ? Optional.of(token.getToken().getSubject())
                : Optional.empty();
    }
}
