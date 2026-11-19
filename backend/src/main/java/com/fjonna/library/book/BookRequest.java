package com.fjonna.library.book;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record BookRequest(
    @NotBlank @Size(max = 255) String title,
    @NotBlank @Size(max = 255) String author,
    @Size(max = 100) String genre,
    BookStatus status,
    @PositiveOrZero BigDecimal price,
    @Positive Integer pages,
    @Size(max = 20) String isbn) {
}
