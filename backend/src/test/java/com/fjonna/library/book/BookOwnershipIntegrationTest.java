package com.fjonna.library.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fjonna.library.TestcontainersConfiguration;
import com.fjonna.library.support.TestJwt;
import com.jayway.jsonpath.JsonPath;
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

/**
 * Ownership isolation at the HTTP layer: a book created by one user is only ever visible to, and
 * only ever modifiable by, that same user (or an admin, covered separately).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookOwnershipIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private RequestPostProcessor userA;
    private RequestPostProcessor userB;

    private RequestPostProcessor newUser(String label) {
        UUID id = UUID.randomUUID();
        return TestJwt.user(id, label + "-" + id, label + "@example.com");
    }

    private Long createBook(RequestPostProcessor as, String title, String author) throws Exception {
        String body = mockMvc.perform(post("/api/books")
                .with(as)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + title + "\",\"author\":\"" + author + "\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return Long.valueOf(JsonPath.read(body, "$.id").toString());
    }

    @Test
    void userSeesOnlyTheirOwnBooks() throws Exception {
        RequestPostProcessor a = newUser("alice");
        RequestPostProcessor b = newUser("bob");
        createBook(a, "Dune", "Frank Herbert");
        createBook(a, "Children of Dune", "Frank Herbert");
        createBook(b, "Neuromancer", "William Gibson");

        mockMvc.perform(get("/api/books").with(a))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[*].title").value(org.hamcrest.Matchers.containsInAnyOrder("Dune", "Children of Dune")));

        mockMvc.perform(get("/api/books").with(b))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].title").value("Neuromancer"));
    }

    @Test
    void anotherUserCannotUpdateYourBookAndItStaysUnchanged() throws Exception {
        RequestPostProcessor a = newUser("alice");
        RequestPostProcessor b = newUser("bob");
        Long bookId = createBook(a, "Dune", "Frank Herbert");

        mockMvc.perform(put("/api/books/" + bookId)
                .with(b)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Hijacked Title\",\"author\":\"Nobody\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"));

        mockMvc.perform(get("/api/books").with(a))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].title").value("Dune"))
            .andExpect(jsonPath("$[0].author").value("Frank Herbert"));
    }

    @Test
    void anotherUserCannotDeleteYourBookAndItStillExists() throws Exception {
        RequestPostProcessor a = newUser("alice");
        RequestPostProcessor b = newUser("bob");
        Long bookId = createBook(a, "Dune", "Frank Herbert");

        mockMvc.perform(delete("/api/books/" + bookId).with(b))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));

        String ownerListing = mockMvc.perform(get("/api/books").with(a))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertThat(ownerListing).contains("Dune");
    }
}
