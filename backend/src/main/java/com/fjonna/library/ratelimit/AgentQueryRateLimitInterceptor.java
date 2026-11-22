package com.fjonna.library.ratelimit;

import tools.jackson.databind.ObjectMapper;
import com.fjonna.library.web.GlobalExceptionHandler.ErrorBody;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Limits POST /api/agent/query to {@link #LIMIT} requests per user per {@link #WINDOW}. */
@Component
@RequiredArgsConstructor
public class AgentQueryRateLimitInterceptor implements HandlerInterceptor {

    private static final String ACTION = "agent-query";
    private static final int LIMIT = 30;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String key = currentUserKey();
        TokenBucket.Result result = rateLimiter.tryConsume(ACTION, key, LIMIT, WINDOW);
        if (result.allowed()) {
            return true;
        }
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
        response.setContentType("application/json");
        ErrorBody body = new ErrorBody(HttpStatus.TOO_MANY_REQUESTS.value(), HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
            "Too many questions, too fast. Please wait " + result.retryAfterSeconds() + " seconds and try again.");
        objectMapper.writeValue(response.getWriter(), body);
        return false;
    }

    private String currentUserKey() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }
}
