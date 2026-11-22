package com.fjonna.library.book;

import com.fjonna.library.user.User;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    @Transactional(readOnly = true)
    public List<BookResponse> listForUser(UUID userId) {
        return bookRepository.findByOwner(userId).stream().map(BookResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<BookResponse> listForUser(UUID userId, Pageable pageable) {
        return bookRepository.findByOwner(userId, pageable).stream().map(BookResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<BookResponse> listAll() {
        return bookRepository.findAllWithOwner().stream().map(BookResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<BookResponse> listAll(Pageable pageable) {
        return bookRepository.findAllWithOwner(pageable).stream().map(BookResponse::from).toList();
    }

    @Transactional
    public BookResponse create(User owner, BookRequest request) {
        Book book = new Book();
        book.setUser(owner);
        apply(book, request);
        return BookResponse.from(bookRepository.save(book));
    }

    /** Regular users can only touch their own books; admins can touch any. */
    @Transactional
    public BookResponse update(UUID userId, boolean admin, Long id, BookRequest request) {
        Book book = find(userId, admin, id);
        apply(book, request);
        return BookResponse.from(book);
    }

    @Transactional
    public void delete(UUID userId, boolean admin, Long id) {
        bookRepository.delete(find(userId, admin, id));
    }

    private Book find(UUID userId, boolean admin, Long id) {
        return (admin
                ? bookRepository.findByIdWithOwner(id)
                : bookRepository.findByIdAndOwner(id, userId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
    }

    private void apply(Book book, BookRequest r) {
        book.setTitle(r.title().trim());
        book.setAuthor(r.author().trim());
        book.setGenre(r.genre());
        book.setStatus(r.status() != null ? r.status() : BookStatus.TO_READ);
        book.setPrice(r.price());
        book.setPages(r.pages());
        book.setIsbn(r.isbn());
    }
}
