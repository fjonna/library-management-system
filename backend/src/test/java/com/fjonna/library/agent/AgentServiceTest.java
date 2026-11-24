package com.fjonna.library.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fjonna.library.agent.AgentRepository.Sort;
import com.fjonna.library.agent.AgentRepository.Table;
import com.fjonna.library.agent.QueryParser.Parsed;
import com.fjonna.library.book.BookStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Empty-user (zero books) regression tests for AgentService's own sentence-building logic. */
class AgentServiceTest {

    private final QueryParser parser = mock(QueryParser.class);
    private final AgentRepository repo = mock(AgentRepository.class);
    private final AgentService service = new AgentService(parser, repo);
    private final UUID uid = UUID.randomUUID();

    private void stubParse(Intent intent) {
        when(parser.parse(any(), any(), any()))
            .thenReturn(new Parsed(intent, null, null, null, null, null, null));
        when(repo.genres(any())).thenReturn(List.of());
        when(repo.authors(any())).thenReturn(List.of());
    }

    private void stubParseWithStatus(Intent intent, BookStatus status) {
        when(parser.parse(any(), any(), any()))
            .thenReturn(new Parsed(intent, null, null, null, status, null, null));
        when(repo.genres(any())).thenReturn(List.of());
        when(repo.authors(any())).thenReturn(List.of());
    }

    @Test
    void averagePriceWithNoPricedBooksIsFriendlyNeverNull() {
        stubParse(Intent.AVERAGE_PRICE);
        when(repo.averagePrice(any()))
            .thenReturn(new Table(List.of("Average price", "Priced books"), List.of(List.of(BigDecimal.ZERO, 0L))));

        QueryResult result = service.answer(uid, false, "What is the average price of my books?");

        assertThat(result.summary()).doesNotContainIgnoringCase("null");
        assertThat(result.summary()).contains("You don't have any books with a price yet");
    }

    @Test
    void averagePriceWithPricedBooksStillWorks() {
        stubParse(Intent.AVERAGE_PRICE);
        when(repo.averagePrice(any()))
            .thenReturn(new Table(List.of("Average price", "Priced books"), List.of(List.of(new BigDecimal("12.50"), 2L))));

        QueryResult result = service.answer(uid, false, "What is the average price of my books?");

        assertThat(result.summary()).doesNotContainIgnoringCase("null");
        assertThat(result.summary()).contains("12.50").contains("2 priced books");
    }

    @Test
    void topAuthorWithNoBooksDoesNotThrowAndIsFriendly() {
        stubParse(Intent.TOP_AUTHOR);
        when(repo.topAuthor(any(), anyInt())).thenReturn(new Table(List.of("Author", "Books"), List.of()));

        QueryResult result = service.answer(uid, false, "Which author has the most books?");

        assertThat(result.summary()).doesNotContainIgnoringCase("null");
        assertThat(result.summary()).contains("don't have any books");
    }

    @Test
    void topAuthorNamesBothAuthorsOnATieInEnglish() {
        stubParse(Intent.TOP_AUTHOR);
        when(repo.topAuthor(any(), anyInt())).thenReturn(new Table(List.of("Author", "Books"),
            List.of(List.of("Frank Herbert", 3L), List.of("George Orwell", 3L))));

        QueryResult result = service.answer(uid, false, "Which author has the most books?");

        assertThat(result.summary()).isEqualTo("Frank Herbert and George Orwell are tied with 3 books each in your library.");
    }

    @Test
    void topAuthorNamesTiedAuthorsInAlbanian() {
        stubParse(Intent.TOP_AUTHOR);
        when(repo.topAuthor(any(), anyInt())).thenReturn(new Table(List.of("Author", "Books"),
            List.of(List.of("Frank Herbert", 3L), List.of("George Orwell", 3L))));

        QueryResult result = service.answer(uid, false, "Cili autor ka më shumë libra?");

        assertThat(result.summary()).isEqualTo("Frank Herbert dhe George Orwell kanë nga 3 libra secili te librat e tu.");
    }

    @Test
    void topAuthorStillNamesOnlyTheWinnerWithNoTie() {
        stubParse(Intent.TOP_AUTHOR);
        when(repo.topAuthor(any(), anyInt())).thenReturn(new Table(List.of("Author", "Books"),
            List.of(List.of("Frank Herbert", 5L), List.of("George Orwell", 2L))));

        QueryResult result = service.answer(uid, false, "Which author has the most books?");

        assertThat(result.summary()).isEqualTo("Frank Herbert has the most books in your library (5).");
    }

    @Test
    void mostPopularGenreNamesWinnerScopedToUsersOwnBooks() {
        stubParse(Intent.MOST_POPULAR_GENRE);
        when(repo.genreBreakdown(any())).thenReturn(new Table(List.of("Genre", "Books"),
            List.of(List.of("Fantasy", 5L), List.of("Drama", 2L))));

        QueryResult result = service.answer(uid, false, "What is the most popular genre?");

        assertThat(result.summary()).isEqualTo("Fantasy has the most books (5) among 2 genres.");
    }

    @Test
    void mostPopularGenreNamesAllTiedGenresForAdminAcrossTheWholeLibrary() {
        stubParse(Intent.MOST_POPULAR_GENRE);
        when(repo.genreBreakdown(any())).thenReturn(new Table(List.of("Genre", "Books"),
            List.of(List.of("Drama", 4L), List.of("Fantasy", 4L), List.of("Sci-Fi", 1L))));

        QueryResult result = service.answer(uid, true, "Which genre is most popular?");

        assertThat(result.summary()).isEqualTo("Drama and Fantasy are tied with 4 books each.");
    }

    @Test
    void mostPopularGenreInAlbanian() {
        stubParse(Intent.MOST_POPULAR_GENRE);
        when(repo.genreBreakdown(any())).thenReturn(new Table(List.of("Genre", "Books"),
            List.of(List.of("Fantasy", 5L), List.of("Drama", 2L))));

        QueryResult result = service.answer(uid, false, "Cili është zhanri më i popullarizuar?");

        assertThat(result.summary()).isEqualTo("Fantasy ka më shumë libra (5) mes 2 zhanërve.");
    }

    @Test
    void countBooksWithCompletedStatusAndNoneYetReadsNaturallyInEnglish() {
        stubParseWithStatus(Intent.COUNT_BOOKS, BookStatus.COMPLETED);
        when(repo.count(any())).thenReturn(new Table(List.of("Books"), List.of(List.of(0L))));

        QueryResult result = service.answer(uid, false, "How many books have I completed?");

        assertThat(result.summary()).isEqualTo("You have 0 completed books.");
        assertThat(result.summary()).doesNotContain("Filters").doesNotContain("COMPLETED").doesNotContain("status=");
    }

    @Test
    void countBooksWithCompletedStatusAndNoneYetReadsNaturallyInAlbanian() {
        stubParseWithStatus(Intent.COUNT_BOOKS, BookStatus.COMPLETED);
        when(repo.count(any())).thenReturn(new Table(List.of("Books"), List.of(List.of(0L))));

        QueryResult result = service.answer(uid, false, "Sa libra kam përfunduar?");

        assertThat(result.summary()).isEqualTo("Nuk ke asnjë libër të përfunduar.");
    }

    @Test
    void mostBooksOwnerNamesBothOwnersOnATieInEnglish() {
        stubParse(Intent.MOST_BOOKS_OWNER);
        when(repo.topOwners(anyInt())).thenReturn(new Table(List.of("User", "Books"),
            List.of(List.of("fjofjo", 2L), List.of("xhesi", 2L))));

        QueryResult result = service.answer(uid, true, "Who owns the most books?");

        assertThat(result.summary()).isEqualTo("fjofjo and xhesi are tied with 2 books each.");
    }

    @Test
    void mostBooksOwnerNamesAllTiedOwnersOnATieOfThreeInAlbanian() {
        stubParse(Intent.MOST_BOOKS_OWNER);
        when(repo.topOwners(anyInt())).thenReturn(new Table(List.of("User", "Books"),
            List.of(List.of("a", 2L), List.of("b", 2L), List.of("c", 2L))));

        QueryResult result = service.answer(uid, true, "Kush ka më shumë libra?");

        assertThat(result.summary()).isEqualTo("a, b dhe c kanë nga 2 libra secili.");
    }

    @Test
    void mostBooksOwnerStillNamesOnlyTheWinnerWithNoTie() {
        stubParse(Intent.MOST_BOOKS_OWNER);
        when(repo.topOwners(anyInt())).thenReturn(new Table(List.of("User", "Books"),
            List.of(List.of("fjofjo", 5L), List.of("xhesi", 2L))));

        QueryResult result = service.answer(uid, true, "Who owns the most books?");

        assertThat(result.summary()).isEqualTo("fjofjo owns the most books (5).");
    }

    @Test
    void mostPopularBookNamesBothTitlesOnATieInEnglish() {
        stubParse(Intent.MOST_POPULAR_BOOK);
        when(repo.popularBooks(anyInt())).thenReturn(new Table(List.of("Title", "Author", "Owners"),
            List.of(List.of("Dune", "Frank Herbert", 2L), List.of("1984", "George Orwell", 2L))));

        QueryResult result = service.answer(uid, true, "Which is the most popular book?");

        assertThat(result.summary()).isEqualTo("'Dune' and '1984' are tied, each owned by 2 users.");
    }

    @Test
    void mostPopularBookNamesTiedTitlesInAlbanian() {
        stubParse(Intent.MOST_POPULAR_BOOK);
        when(repo.popularBooks(anyInt())).thenReturn(new Table(List.of("Title", "Author", "Owners"),
            List.of(List.of("Dune", "Frank Herbert", 2L), List.of("1984", "George Orwell", 2L))));

        QueryResult result = service.answer(uid, true, "Cili është libri më i popullarizuar?");

        assertThat(result.summary()).isEqualTo("'Dune' dhe '1984' zotërohen nga 2 përdorues secili.");
    }

    @Test
    void topExpensiveBooksAreShownBelowInsteadOfNarratingOneRow() {
        stubParse(Intent.TOP_EXPENSIVE);
        when(repo.books(any(), eq(false), eq(Sort.PRICE_DESC), anyInt()))
            .thenReturn(new Table(List.of("Title"), List.of(List.of("Dune"), List.of("Foo"))));

        QueryResult result = service.answer(uid, false, "Show the five most expensive books");

        assertThat(result.summary()).isEqualTo("Your 2 most expensive books are shown below.");
    }

    /** The legacy parser cannot express "sum of pages per user" as an Intent, so it reports UNKNOWN. */
    private void stubUnknownWithSpec(QuerySpec spec) {
        when(parser.parse(any(), any(), any()))
            .thenReturn(new Parsed(Intent.UNKNOWN, null, null, null, null, null, null));
        when(parser.toSpec(any(), any(), any())).thenReturn(spec);
        when(repo.genres(any())).thenReturn(List.of());
        when(repo.authors(any())).thenReturn(List.of());
    }

    @Test
    void groupByUserQuestionIsAdminOnlyForRegularUsers() {
        stubUnknownWithSpec(new QuerySpec(Aggregate.SUM, Field.PAGES, QuerySpec.Filters.NONE, GroupBy.USER, null, null));

        QueryResult result = service.answer(uid, false, "Which user has the most pages?");

        assertThat(result.suggestedQuestions()).isTrue();
        assertThat(result.summary()).contains("only admins can see that");
    }

    @Test
    void groupByUserQuestionRunsGenericEngineForAdmin() {
        stubUnknownWithSpec(new QuerySpec(Aggregate.SUM, Field.PAGES, QuerySpec.Filters.NONE, GroupBy.USER, null, null));
        when(repo.run(any())).thenReturn(new Table(List.of("User", "Pages"), List.of(List.of("alice", 900L))));

        QueryResult result = service.answer(uid, true, "Which user has the most pages?");

        assertThat(result.suggestedQuestions()).isFalse();
        assertThat(result.summary()).contains("alice");
    }

    @Test
    void sumOfPagesQuestionRunsGenericEngineForRegularUser() {
        stubUnknownWithSpec(new QuerySpec(Aggregate.SUM, Field.PAGES, QuerySpec.Filters.NONE, null, null, null));
        when(repo.run(any())).thenReturn(new Table(List.of("Value", "Count"), List.of(List.of(3450L, 12L))));

        QueryResult result = service.answer(uid, false, "How many pages do all my books have together?");

        assertThat(result.suggestedQuestions()).isFalse();
        assertThat(result.summary()).doesNotContainIgnoringCase("null");
        assertThat(result.summary()).contains("3,450").contains("12 books");
    }

    @Test
    void unknownQuestionCloseToAnExampleSuggestsItByName() {
        stubUnknownWithSpec(new QuerySpec(Aggregate.LIST, null, QuerySpec.Filters.NONE, null, null, null));

        QueryResult result = service.answer(uid, false, "Show the five most expnsive books");

        assertThat(result.suggestedQuestions()).isTrue();
        assertThat(result.summary()).isEqualTo("Did you mean: Show the five most expensive books?");
    }

    @Test
    void trulyUnrelatedQuestionFallsBackToGenericExamples() {
        stubUnknownWithSpec(new QuerySpec(Aggregate.LIST, null, QuerySpec.Filters.NONE, null, null, null));

        QueryResult result = service.answer(uid, false, "What is the weather today?");

        assertThat(result.suggestedQuestions()).isTrue();
        assertThat(result.summary()).isEqualTo("Try one of these questions.");
    }
}
