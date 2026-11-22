package com.fjonna.library.agent;

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

/** POST /api/agent/query over HTTP: a real result table, the admin-only guard, and request validation. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgentQueryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private RequestPostProcessor newUser(String label) {
        UUID id = UUID.randomUUID();
        return TestJwt.user(id, label + "-" + id, label + "@example.com");
    }

    private void createBook(RequestPostProcessor as, String title, String author, String price) throws Exception {
        mockMvc.perform(post("/api/books")
                .with(as)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + title + "\",\"author\":\"" + author + "\",\"price\":" + price + "}"))
            .andExpect(status().isCreated());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder ask(RequestPostProcessor as, String question) {
        return post("/api/agent/query")
            .with(as)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"question\":\"" + question + "\"}");
    }

    @Test
    void mostExpensiveBooksReturnsARealTable() throws Exception {
        RequestPostProcessor user = newUser("alice");
        createBook(user, "Dune", "Frank Herbert", "40.00");
        createBook(user, "Project Hail Mary", "Andy Weir", "25.00");

        mockMvc.perform(ask(user, "Show the five most expensive books"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.intent").value("TOP_EXPENSIVE"))
            .andExpect(jsonPath("$.suggestedQuestions").value(false))
            .andExpect(jsonPath("$.columns").isNotEmpty())
            .andExpect(jsonPath("$.rows.length()").value(2))
            .andExpect(jsonPath("$.rows[0][1]").value("Dune"));
    }

    @Test
    void regularUserAskingWhoOwnsTheMostBooksGetsTheNotAvailableAnswer() throws Exception {
        RequestPostProcessor user = newUser("bob");

        mockMvc.perform(ask(user, "Who owns the most books?"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.suggestedQuestions").value(true))
            .andExpect(jsonPath("$.summary")
                .value(org.hamcrest.Matchers.containsString("only admins can see that")));
    }

    @Test
    void adminAskingWhoOwnsTheMostBooksGetsARealAnswer() throws Exception {
        RequestPostProcessor admin = TestJwt.admin(UUID.randomUUID(), "root", "root@example.com");

        mockMvc.perform(ask(admin, "Who owns the most books?"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.intent").value("MOST_BOOKS_OWNER"))
            .andExpect(jsonPath("$.suggestedQuestions").value(false));
    }

    @Test
    void blankQuestionReturns400WithConsistentBody() throws Exception {
        RequestPostProcessor user = newUser("carol");

        mockMvc.perform(ask(user, ""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"));
    }
}
