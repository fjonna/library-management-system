package com.fjonna.library.ratelimit;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fjonna.library.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** POST /api/agent/query is limited to 30 requests per minute per user; the 31st must get 429 + Retry-After. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AgentQueryRateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void thirtyFirstRequestWithinAMinuteIsRateLimited() throws Exception {
        UUID userId = UUID.randomUUID();

        for (int i = 0; i < 30; i++) {
            mockMvc.perform(request(userId)).andExpect(status().isOk());
        }

        mockMvc.perform(request(userId))
            .andExpect(status().isTooManyRequests())
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.status").value(429))
            .andExpect(jsonPath("$.error").value("Too Many Requests"))
            .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void differentUsersHaveIndependentLimits() throws Exception {
        UUID exhaustedUser = UUID.randomUUID();
        for (int i = 0; i < 30; i++) {
            mockMvc.perform(request(exhaustedUser)).andExpect(status().isOk());
        }
        mockMvc.perform(request(exhaustedUser)).andExpect(status().isTooManyRequests());

        UUID freshUser = UUID.randomUUID();
        mockMvc.perform(request(freshUser)).andExpect(status().isOk());
    }

    private MockHttpServletRequestBuilder request(UUID userId) {
        return post("/api/agent/query")
            .with(jwt().jwt(j -> j.subject(userId.toString())).authorities(new SimpleGrantedAuthority("ROLE_USER")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"question\":\"How many books do I have?\"}");
    }
}
