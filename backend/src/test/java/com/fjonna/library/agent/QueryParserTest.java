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
    void mostPopularGenreEnglishIsNotMisroutedToMostPopularBook() {
        assertThat(parse("What is the most popular genre?").intent()).isEqualTo(Intent.MOST_POPULAR_GENRE);
    }

    @Test
    void mostCommonGenreEnglish() {
        assertThat(parse("What's the most common genre in my library?").intent()).isEqualTo(Intent.MOST_POPULAR_GENRE);
    }

    @Test
    void mostPopularGenreAlbanian() {
        assertThat(parse("Cili është zhanri më i popullarizuar?").intent()).isEqualTo(Intent.MOST_POPULAR_GENRE);
    }

    @Test
    void whichGenreHasTheMostBooksAlbanian() {
        assertThat(parse("Cili zhanër ka më shumë libra?").intent()).isEqualTo(Intent.MOST_POPULAR_GENRE);
    }

    @Test
    void plainGenreBreakdownIsNotMisroutedToMostPopularGenre() {
        assertThat(parse("How many books per genre?").intent()).isEqualTo(Intent.GENRE_BREAKDOWN);
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

    @Test
    void booksUnderPriceEnglish() {
        QueryParser.Parsed p = parse("Show books under $20");
        assertThat(p.intent()).isEqualTo(Intent.BOOKS_UNDER_PRICE);
        assertThat(p.price()).isEqualByComparingTo("20");
    }

    @Test
    void booksUnderPriceAlbanian() {
        QueryParser.Parsed p = parse("Libra nën 15 euro");
        assertThat(p.intent()).isEqualTo(Intent.BOOKS_UNDER_PRICE);
        assertThat(p.price()).isEqualByComparingTo("15");
    }

    @Test
    void averagePriceEnglish() {
        assertThat(parse("What is the average price of my books?").intent()).isEqualTo(Intent.AVERAGE_PRICE);
    }

    @Test
    void averagePriceAlbanian() {
        assertThat(parse("Sa është çmimi mesatar i librave?").intent()).isEqualTo(Intent.AVERAGE_PRICE);
    }

    @Test
    void topAuthorEnglish() {
        assertThat(parse("Which author has the most books?").intent()).isEqualTo(Intent.TOP_AUTHOR);
    }

    @Test
    void topAuthorAlbanian() {
        assertThat(parse("Cili autor ka më shumë libra?").intent()).isEqualTo(Intent.TOP_AUTHOR);
    }

    @Test
    void recentBooksEnglish() {
        assertThat(parse("Show my most recent books").intent()).isEqualTo(Intent.RECENT_BOOKS);
    }

    @Test
    void recentBooksAlbanian() {
        assertThat(parse("Librat më të fundit").intent()).isEqualTo(Intent.RECENT_BOOKS);
    }

    @Test
    void searchTitleEnglish() {
        QueryParser.Parsed p = parse("Find the book titled Dune");
        assertThat(p.intent()).isEqualTo(Intent.SEARCH_TITLE);
        assertThat(p.searchText()).isEqualTo("dune");
    }

    @Test
    void searchTitleAlbanian() {
        QueryParser.Parsed p = parse("Kërko librin me titullin Dune");
        assertThat(p.intent()).isEqualTo(Intent.SEARCH_TITLE);
        assertThat(p.searchText()).isEqualTo("dune");
    }

    @Test
    void ownerOfMostBooksAlbanian() {
        assertThat(parse("Kush ka më shumë libra?").intent()).isEqualTo(Intent.MOST_BOOKS_OWNER);
    }

    @Test
    void cheapestAlbanian() {
        assertThat(parse("Cilat janë librat më të lirë?").intent()).isEqualTo(Intent.CHEAPEST);
    }

    @Test
    void mostExpensiveAlbanian() {
        assertThat(parse("Cilat janë librat më të shtrenjtë?").intent()).isEqualTo(Intent.TOP_EXPENSIVE);
    }

    @Test
    void countBooksAlbanian() {
        assertThat(parse("Sa libra kam?").intent()).isEqualTo(Intent.COUNT_BOOKS);
    }
}
