package com.fjonna.library.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.fjonna.library.book.BookStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpecParserTest {

    private final SpecParser parser = new SpecParser();
    private final List<String> genres = List.of("fantasy", "sci-fi", "history");
    private final List<String> authors = List.of("george orwell", "frank herbert", "j.r.r. tolkien");

    private QuerySpec parse(String q) {
        return parser.parse(q, genres, authors);
    }

    @Test
    void sumOfPagesTogetherEnglish() {
        QuerySpec s = parse("How many pages do all my books have together?");
        assertThat(s.aggregate()).isEqualTo(Aggregate.SUM);
        assertThat(s.field()).isEqualTo(Field.PAGES);
    }

    @Test
    void sumOfPagesTogetherAlbanian() {
        QuerySpec s = parse("Sa faqe kane te gjithe librat bashke?");
        assertThat(s.aggregate()).isEqualTo(Aggregate.SUM);
        assertThat(s.field()).isEqualTo(Field.PAGES);
    }

    @Test
    void averagePagesOfFantasyBooksEnglish() {
        QuerySpec s = parse("Average pages of my fantasy books");
        assertThat(s.aggregate()).isEqualTo(Aggregate.AVG);
        assertThat(s.field()).isEqualTo(Field.PAGES);
        assertThat(s.filters().genre()).isEqualTo("fantasy");
    }

    @Test
    void averagePagesOfFantasyBooksAlbanian() {
        QuerySpec s = parse("Sa faqe ka mesatarisht nje liber fantasy?");
        assertThat(s.aggregate()).isEqualTo(Aggregate.AVG);
        assertThat(s.field()).isEqualTo(Field.PAGES);
        assertThat(s.filters().genre()).isEqualTo("fantasy");
    }

    @Test
    void averagePriceOfFantasyBooks() {
        QuerySpec s = parse("What is the average price of my fantasy books?");
        assertThat(s.aggregate()).isEqualTo(Aggregate.AVG);
        assertThat(s.field()).isEqualTo(Field.PRICE);
        assertThat(s.filters().genre()).isEqualTo("fantasy");
    }

    @Test
    void countBooksByAuthor() {
        QuerySpec s = parse("How many books by Orwell do I have?");
        assertThat(s.aggregate()).isEqualTo(Aggregate.COUNT);
        assertThat(s.filters().author()).isEqualTo("george orwell");
    }

    @Test
    void countBooksByAuthorAlbanian() {
        QuerySpec s = parse("Sa libra kam nga Orwell?");
        assertThat(s.aggregate()).isEqualTo(Aggregate.COUNT);
        assertThat(s.filters().author()).isEqualTo("george orwell");
    }

    @Test
    void booksOverPagesThresholdEnglish() {
        QuerySpec s = parse("Show books over 300 pages");
        assertThat(s.filters().pagesMin()).isEqualTo(300);
        assertThat(s.filters().pagesMax()).isNull();
    }

    @Test
    void booksOverPagesThresholdAlbanian() {
        QuerySpec s = parse("Librat mbi 300 faqe");
        assertThat(s.filters().pagesMin()).isEqualTo(300);
    }

    @Test
    void booksBetweenPricesEnglish() {
        QuerySpec s = parse("Show books between 10 and 20 dollars");
        assertThat(s.filters().priceMin()).isEqualByComparingTo("10");
        assertThat(s.filters().priceMax()).isEqualByComparingTo("20");
    }

    @Test
    void booksBetweenPricesAlbanian() {
        QuerySpec s = parse("Libra mes 10 dhe 20 euro");
        assertThat(s.filters().priceMin()).isEqualByComparingTo("10");
        assertThat(s.filters().priceMax()).isEqualByComparingTo("20");
    }

    @Test
    void cheapestCompletedBookEnglish() {
        QuerySpec s = parse("Which is my cheapest completed book?");
        assertThat(s.orderBy()).isEqualTo(new QuerySpec.OrderBy(Field.PRICE, QuerySpec.Direction.ASC));
        assertThat(s.filters().status()).isEqualTo(BookStatus.COMPLETED);
    }

    @Test
    void cheapestCompletedBookAlbanian() {
        QuerySpec s = parse("Cili eshte libri me i liri qe kam lexuar?");
        assertThat(s.orderBy()).isEqualTo(new QuerySpec.OrderBy(Field.PRICE, QuerySpec.Direction.ASC));
        assertThat(s.filters().status()).isEqualTo(BookStatus.COMPLETED);
    }

    @Test
    void unreadBooksCountEnglish() {
        QuerySpec s = parse("How many books are unread?");
        assertThat(s.aggregate()).isEqualTo(Aggregate.COUNT);
        assertThat(s.filters().status()).isEqualTo(BookStatus.TO_READ);
    }

    @Test
    void unreadBooksCountAlbanian() {
        QuerySpec s = parse("Sa libra nuk i kam lexuar?");
        assertThat(s.filters().status()).isEqualTo(BookStatus.TO_READ);
    }

    @Test
    void totalValueOfFantasyBooks() {
        QuerySpec s = parse("Total value of my fantasy books");
        assertThat(s.aggregate()).isEqualTo(Aggregate.SUM);
        assertThat(s.field()).isEqualTo(Field.PRICE);
        assertThat(s.filters().genre()).isEqualTo("fantasy");
    }

    @Test
    void longestBookByAuthor() {
        QuerySpec s = parse("Longest book by Tolkien");
        assertThat(s.orderBy()).isEqualTo(new QuerySpec.OrderBy(Field.PAGES, QuerySpec.Direction.DESC));
        assertThat(s.filters().author()).isEqualTo("j.r.r. tolkien");
    }

    @Test
    void booksPerAuthorEnglish() {
        QuerySpec s = parse("Books per author");
        assertThat(s.groupBy()).isEqualTo(GroupBy.AUTHOR);
    }

    @Test
    void booksPerAuthorAlbanian() {
        QuerySpec s = parse("Sa libra kam per autor?");
        assertThat(s.groupBy()).isEqualTo(GroupBy.AUTHOR);
    }

    @Test
    void threeShortestBooks() {
        QuerySpec s = parse("Show the 3 shortest books");
        assertThat(s.orderBy()).isEqualTo(new QuerySpec.OrderBy(Field.PAGES, QuerySpec.Direction.ASC));
        assertThat(s.limit()).isEqualTo(3);
    }

    @Test
    void whoOwnsTheMostBooksGroupsByUserAsPlainCount() {
        QuerySpec s = parse("Who owns the most books?");
        assertThat(s.groupBy()).isEqualTo(GroupBy.USER);
        assertThat(s.field()).isNull();
        assertThat(s.aggregate()).isEqualTo(Aggregate.GROUP_COUNT);
    }

    @Test
    void howManyBooksDoesEachUserHave() {
        QuerySpec s = parse("How many books does each user have?");
        assertThat(s.groupBy()).isEqualTo(GroupBy.USER);
        assertThat(s.field()).isNull();
    }

    @Test
    void whichUserHasTheMostPagesGroupsByUserWithPagesField() {
        QuerySpec s = parse("Which user has the most pages?");
        assertThat(s.groupBy()).isEqualTo(GroupBy.USER);
        assertThat(s.field()).isEqualTo(Field.PAGES);
        assertThat(s.aggregate()).isEqualTo(Aggregate.SUM);
    }

    @Test
    void typoToleranceStillMatchesExpensiveAndFantasy() {
        QuerySpec s = parse("Show the most expesnive fantsy books");
        assertThat(s.orderBy()).isEqualTo(new QuerySpec.OrderBy(Field.PRICE, QuerySpec.Direction.DESC));
        assertThat(s.filters().genre()).isEqualTo("fantasy");
    }

    @Test
    void emptySpecDetectedForUnrelatedQuestion() {
        QuerySpec s = parse("What is the weather today?");
        assertThat(parser.isEmpty(s)).isTrue();
    }

    @Test
    void nonEmptySpecForRecognizedQuestion() {
        QuerySpec s = parse("Show my sci-fi books");
        assertThat(parser.isEmpty(s)).isFalse();
    }
}
