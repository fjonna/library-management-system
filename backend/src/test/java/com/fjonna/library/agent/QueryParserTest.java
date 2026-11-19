package com.fjonna.library.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.fjonna.library.book.BookStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class QueryParserTest {

    private final QueryParser parser = new QueryParser();
    private final List<String> genres = List.of("sci-fi", "history");
    private final List<String> authors = List.of("frank herbert");

    private QueryParser.Parsed parse(String q) {
        return parser.parse(q, genres, authors);
    }

    @Test
    void ownerOfMostBooks() {
        assertThat(parse("Who owns the most books?").intent()).isEqualTo(Intent.MOST_BOOKS_OWNER);
    }

    @Test
    void mostPopularBook() {
        assertThat(parse("Which is the most popular book?").intent()).isEqualTo(Intent.MOST_POPULAR_BOOK);
    }

    @Test
    void fiveMostExpensive() {
        QueryParser.Parsed p = parse("Show the five most expensive books");
        assertThat(p.intent()).isEqualTo(Intent.TOP_EXPENSIVE);
        assertThat(p.limit()).isEqualTo(5);
    }

    @Test
    void cheapestWithDigit() {
        QueryParser.Parsed p = parse("cheapest 3 books");
        assertThat(p.intent()).isEqualTo(Intent.CHEAPEST);
        assertThat(p.limit()).isEqualTo(3);
    }

    @Test
    void genreFilter() {
        QueryParser.Parsed p = parse("show my sci-fi books");
        assertThat(p.intent()).isEqualTo(Intent.LIST_BOOKS);
        assertThat(p.genre()).isEqualTo("sci-fi");
    }

    @Test
    void authorFilter() {
        assertThat(parse("books by Frank Herbert").author()).isEqualTo("frank herbert");
    }

    @Test
    void countWithStatus() {
        QueryParser.Parsed p = parse("How many books have I completed?");
        assertThat(p.intent()).isEqualTo(Intent.COUNT_BOOKS);
        assertThat(p.status()).isEqualTo(BookStatus.COMPLETED);
    }

    @Test
    void unknownQuestion() {
        assertThat(parse("what is the weather").intent()).isEqualTo(Intent.UNKNOWN);
    }
}
