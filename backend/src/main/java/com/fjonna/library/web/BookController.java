package com.fjonna.library.web;

import com.fjonna.library.book.BookRequest;
import com.fjonna.library.book.BookResponse;
import com.fjonna.library.book.BookService;
import com.fjonna.library.user.User;
import com.fjonna.library.user.UserService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final UserService userService;

    @GetMapping
    public List<BookResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return bookService.listForUser(UUID.fromString(jwt.getSubject()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody BookRequest request) {
        User owner = userService.syncFromJwt(jwt);
        return bookService.create(owner, request);
    }

    @PutMapping("/{id}")
    public BookResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                               @Valid @RequestBody BookRequest request) {
        return bookService.update(UUID.fromString(jwt.getSubject()), false, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        bookService.delete(UUID.fromString(jwt.getSubject()), false, id);
    }
}
