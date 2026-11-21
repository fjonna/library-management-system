package com.fjonna.library.insight;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class InsightController {

    private final InsightService insightService;

    @GetMapping("/api/insights")
    public InsightResponse myInsights(@AuthenticationPrincipal Jwt jwt) {
        return insightService.insights(UUID.fromString(jwt.getSubject()));
    }

    @PostMapping("/api/insights/summary")
    public InsightSummaryResponse mySummary(@AuthenticationPrincipal Jwt jwt) {
        return new InsightSummaryResponse(insightService.summary(UUID.fromString(jwt.getSubject())));
    }

    /** Admin only (enforced in SecurityConfig: /api/admin/** requires ROLE_ADMIN). */
    @GetMapping("/api/admin/insights/{userId}")
    public InsightResponse userInsights(@PathVariable UUID userId) {
        return insightService.insights(userId);
    }

    @PostMapping("/api/admin/insights/{userId}/summary")
    public InsightSummaryResponse userSummary(@PathVariable UUID userId) {
        return new InsightSummaryResponse(insightService.summary(userId));
    }
}
