package com.fjonna.library.book;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BookResponse(
    Long id,
    String title,
    String author,
    String genre,
    BookStatus status,
    BigDecimal price,
    Integer pages,
    String isbn,
    UUID ownerId,
    String ownerUsername,
    LocalDateTime createdAt) {

    public static BookResponse from(Book b) {
        return new BookResponse(
            b.getId(), b.getTitle(), b.getAuthor(), b.getGenre(), b.getStatus(),
            b.getPrice(), b.getPages(), b.getIsbn(),
            b.getUser().getId(), b.getUser().getUsername(), b.getCreatedAt());
    }
}
