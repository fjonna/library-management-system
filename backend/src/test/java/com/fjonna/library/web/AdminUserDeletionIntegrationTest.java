package com.fjonna.library.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fjonna.library.TestcontainersConfiguration;
import com.fjonna.library.book.Book;
import com.fjonna.library.book.BookRepository;
import com.fjonna.library.keycloakadmin.KeycloakAdminClient;
import com.fjonna.library.keycloakadmin.KeycloakAdminException;
import com.fjonna.library.support.TestJwt;
import com.fjonna.library.user.User;
import com.fjonna.library.user.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * DELETE /api/admin/users/{id} now also removes the Keycloak login account, via a mocked
 * {@link KeycloakAdminClient} bean - real HTTP behavior of that client is covered separately by
 * {@link com.fjonna.library.keycloakadmin.RestClientKeycloakAdminClientTest}.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminUserDeletionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private KeycloakAdminClient keycloakAdminClient;

    private User seedUserWithABook() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("target-" + user.getId());
        userRepository.saveAndFlush(user);

        Book book = new Book();
        book.setUser(user);
        book.setTitle("Dune");
        book.setAuthor("Frank Herbert");
        bookRepository.saveAndFlush(book);
        // Detach it: books are removed by the database's own ON DELETE CASCADE, not by Hibernate,
        // so a still-managed Book left pointing at a User this test later deletes would make
        // Hibernate's own flush-time consistency check (wrongly) think that User is still needed.
        entityManager.detach(book);

        return user;
    }

    private RequestPostProcessor admin(UUID id) {
        return TestJwt.admin(id, "admin-" + id, "admin@example.com");
    }

    @Test
    void deletingAUserCallsKeycloakThenRemovesTheLocalProfileAndBooks() throws Exception {
        User target = seedUserWithABook();

        mockMvc.perform(delete("/api/admin/users/" + target.getId()).with(admin(UUID.randomUUID())))
            .andExpect(status().isNoContent());

        verify(keycloakAdminClient).deleteUser(target.getId().toString());
        assertThat(userRepository.existsById(target.getId())).isFalse();
        assertThat(bookRepository.findByOwner(target.getId())).isEmpty();
    }

    @Test
    void anAdminCannotDeleteThemselvesAndKeycloakIsNeverCalled() throws Exception {
        UUID adminId = UUID.randomUUID();

        mockMvc.perform(delete("/api/admin/users/" + adminId).with(admin(adminId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(keycloakAdminClient);
    }

    @Test
    void aKeycloakFailureLeavesLocalDataIntactAndReturns502() throws Exception {
        User target = seedUserWithABook();
        doThrow(new KeycloakAdminException("boom", null)).when(keycloakAdminClient).deleteUser(any());

        mockMvc.perform(delete("/api/admin/users/" + target.getId()).with(admin(UUID.randomUUID())))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.status").value(502))
            .andExpect(jsonPath("$.message").value("Could not reach the login system right now. Please try again shortly."));

        assertThat(userRepository.existsById(target.getId())).isTrue();
        assertThat(bookRepository.findByOwner(target.getId())).hasSize(1);
    }
}
