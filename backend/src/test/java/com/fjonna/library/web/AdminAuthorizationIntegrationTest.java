package com.fjonna.library.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fjonna.library.TestcontainersConfiguration;
import com.fjonna.library.support.TestJwt;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** /api/admin/** requires ROLE_ADMIN (see SecurityConfig): 403 for a plain user, 200 for an admin, 401 with no token. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void adminBooksForbiddenForUser() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/api/admin/books").with(TestJwt.user(id, "plain-" + id, "plain@example.com")))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminBooksOkForAdmin() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/api/admin/books").with(TestJwt.admin(id, "admin-" + id, "admin@example.com")))
            .andExpect(status().isOk());
    }

    @Test
    void adminBooksUnauthorizedWithNoToken() throws Exception {
        mockMvc.perform(get("/api/admin/books"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void adminUsersForbiddenForUser() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/api/admin/users").with(TestJwt.user(id, "plain-" + id, "plain@example.com")))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminUsersOkForAdmin() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/api/admin/users").with(TestJwt.admin(id, "admin-" + id, "admin@example.com")))
            .andExpect(status().isOk());
    }

    @Test
    void adminUsersUnauthorizedWithNoToken() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isUnauthorized());
    }
}
