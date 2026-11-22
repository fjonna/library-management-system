package com.fjonna.library.book;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fjonna.library.TestcontainersConfiguration;
import com.fjonna.library.user.User;
import com.fjonna.library.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** GET /api/books: optional page/size, defaulting to today's "everything" behavior when omitted. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookPaginationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BookRepository bookRepository;

    private User seedUserWithBooks(int count) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setUsername("paging-" + u.getId());
        userRepository.save(u);
        for (int i = 0; i < count; i++) {
            Book b = new Book();
            b.setUser(u);
            b.setTitle("Book " + i);
            b.setAuthor("Author " + i);
            bookRepository.save(b);
        }
        return u;
    }

    @Test
    void omittingPageAndSizeReturnsEverything() throws Exception {
        User u = seedUserWithBooks(5);

        mockMvc.perform(get("/api/books")
                .with(jwt().jwt(j -> j.subject(u.getId().toString())).authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    void sizeLimitsTheNumberOfResults() throws Exception {
        User u = seedUserWithBooks(5);

        mockMvc.perform(get("/api/books").param("size", "2")
                .with(jwt().jwt(j -> j.subject(u.getId().toString())).authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void secondPageReturnsTheRemainder() throws Exception {
        User u = seedUserWithBooks(5);

        mockMvc.perform(get("/api/books").param("page", "1").param("size", "2")
                .with(jwt().jwt(j -> j.subject(u.getId().toString())).authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/books").param("page", "2").param("size", "2")
                .with(jwt().jwt(j -> j.subject(u.getId().toString())).authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void oversizedSizeIsClampedTo100NotRejected() throws Exception {
        User u = seedUserWithBooks(3);

        mockMvc.perform(get("/api/books").param("size", "1000")
                .with(jwt().jwt(j -> j.subject(u.getId().toString())).authorities(new SimpleGrantedAuthority("ROLE_USER"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3));
    }
}
