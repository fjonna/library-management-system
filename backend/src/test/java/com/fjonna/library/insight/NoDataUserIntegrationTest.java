package com.fjonna.library.insight;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fjonna.library.TestcontainersConfiguration;
import com.fjonna.library.support.TestJwt;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/** A brand-new user with zero books must get a friendly 200 everywhere, never a 404/500 from empty aggregates. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NoDataUserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void insightsRecommendationsAndAgentAllOkForAUserWithNoBooks() throws Exception {
        UUID id = UUID.randomUUID();
        RequestPostProcessor newUser = TestJwt.user(id, "newcomer-" + id, "newcomer@example.com");

        mockMvc.perform(get("/api/insights").with(newUser))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stats.totalBooks").value(0));

        mockMvc.perform(get("/api/recommendations").with(newUser))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/agent/query")
                .with(newUser)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":\"How many books do I have?\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.summary").exists());
    }
}
