package com.fjonna.library.web;

import com.fjonna.library.agent.AgentService;
import com.fjonna.library.agent.QueryRequest;
import com.fjonna.library.agent.QueryResult;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    /** Users are answered from their own books; admins from the whole library. */
    @PostMapping("/query")
    public QueryResult query(@AuthenticationPrincipal Jwt jwt, Authentication auth,
                             @Valid @RequestBody QueryRequest request) {
        boolean admin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return agentService.answer(UUID.fromString(jwt.getSubject()), admin, request.question());
    }

    @GetMapping("/examples")
    public List<String> examples() {
        return AgentService.EXAMPLES;
    }
}
