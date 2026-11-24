package com.fjonna.library.web;

import com.fjonna.library.book.BookRequest;
import com.fjonna.library.book.BookResponse;
import com.fjonna.library.book.BookService;
import com.fjonna.library.keycloakadmin.KeycloakAdminClient;
import com.fjonna.library.user.User;
import com.fjonna.library.user.UserRepository;
import com.fjonna.library.user.UserSummary;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Everything under /api/admin requires ROLE_ADMIN (see SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final BookService bookService;
    private final UserRepository userRepository;
    private final KeycloakAdminClient keycloakAdminClient;

    /** page/size are optional; omitting both keeps today's "return everything" behavior for the frontend. */
    @GetMapping("/books")
    public List<BookResponse> allBooks(@RequestParam(required = false) Integer page,
                                       @RequestParam(required = false) Integer size) {
        if (page == null && size == null) {
            return bookService.listAll();
        }
        return bookService.listAll(Paging.of(page, size));
    }

    @PutMapping("/books/{id}")
    public BookResponse updateBook(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
        return bookService.update(null, true, id, request);
    }

    @DeleteMapping("/books/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable Long id) {
        bookService.delete(null, true, id);
    }

    /** page/size are optional; omitting both keeps today's "return everything" behavior for the frontend. */
    @GetMapping("/users")
    public List<UserSummary> allUsers(@RequestParam(required = false) Integer page,
                                      @RequestParam(required = false) Integer size) {
        if (page == null && size == null) {
            return userRepository.findAllSummaries();
        }
        return userRepository.findAllSummaries(Paging.of(page, size));
    }

    /**
     * Permanently deletes the user: first their Keycloak login account, then their local profile
     * and books. Order matters - if the Keycloak call fails, local data is left untouched (the
     * exception propagates straight to {@link GlobalExceptionHandler}, which maps it to a 502,
     * before either repository call below ever runs).
     */
    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        if (id.toString().equals(jwt.getSubject())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete yourself");
        }
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        // The local user's id IS the Keycloak user id (the JWT "sub" claim - see
        // UserService#syncFromJwt), so there's no separate id to look up.
        keycloakAdminClient.deleteUser(user.getId().toString());
        userRepository.delete(user); // cascades to the user's books (ON DELETE CASCADE, see V1 migration)
    }
}
