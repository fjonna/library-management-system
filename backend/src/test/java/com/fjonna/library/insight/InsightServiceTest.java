package com.fjonna.library.insight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fjonna.library.insight.InsightRepository.Averages;
import com.fjonna.library.insight.InsightRepository.BookPrice;
import com.fjonna.library.insight.InsightRepository.Counts;
import com.fjonna.library.insight.InsightRepository.GenreCount;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InsightServiceTest {

    private final InsightRepository repo = mock(InsightRepository.class);
    private final InsightService service = new InsightService(repo);
    private final UUID uid = UUID.randomUUID();

    @Test
    void emptyLibrary() {
        when(repo.counts(uid)).thenReturn(new Counts(0, 0, 0, 0));

        InsightResponse result = service.insights(uid);

        assertThat(result.stats().totalBooks()).isZero();
        assertThat(result.stats().topGenre()).isNull();
        assertThat(result.sentences()).containsExactly("You don't have any books yet.");
        assertThat(service.summary(uid))
            .isEqualTo("You don't have any books in your library yet. Add a few to get personalized insights.");
    }

    @Test
    void oneGenreNoTie() {
        when(repo.counts(uid)).thenReturn(new Counts(3, 1, 1, 1));
        when(repo.averages(uid)).thenReturn(new Averages(new BigDecimal("12.50"), 180.0));
        when(repo.genreCounts(uid)).thenReturn(List.of(new GenreCount("Fantasy", 2), new GenreCount("Drama", 1)));
        when(repo.mostExpensiveBooks(uid)).thenReturn(List.of(new BookPrice("Dune", new BigDecimal("20.00"))));

        InsightResponse result = service.insights(uid);

        assertThat(result.stats().topGenre()).isEqualTo("Fantasy");
        assertThat(result.stats().averagePages()).isEqualTo(180.0);
        assertThat(result.sentences()).containsExactly(
            "Fantasy is your most read genre (2 books).",
            "You tend to read short books (average 180 pages).",
            "You have finished 1 of 3 books.",
            "Your most expensive book is 'Dune' at 20.00.");
    }

    @Test
    void tiedGenresAndLongBooksAndNoPricedBooks() {
        when(repo.counts(uid)).thenReturn(new Counts(4, 2, 1, 1));
        when(repo.averages(uid)).thenReturn(new Averages(null, 500.0));
        when(repo.genreCounts(uid)).thenReturn(List.of(new GenreCount("Drama", 2), new GenreCount("Fantasy", 2)));
        when(repo.mostExpensiveBooks(uid)).thenReturn(List.of());

        InsightResponse result = service.insights(uid);

        assertThat(result.stats().topGenre()).isEqualTo("Drama, Fantasy");
        assertThat(result.stats().averagePrice()).isNull();
        assertThat(result.sentences()).containsExactly(
            "Drama and Fantasy are tied as your most read genres (2 books each).",
            "You tend to read long books (average 500 pages).",
            "You have finished 2 of 4 books.");
    }

    @Test
    void tiedMostExpensiveBooksAreAllNamedInTheSentence() {
        when(repo.counts(uid)).thenReturn(new Counts(3, 1, 1, 1));
        when(repo.averages(uid)).thenReturn(new Averages(new BigDecimal("20.00"), 180.0));
        when(repo.genreCounts(uid)).thenReturn(List.of(new GenreCount("Fantasy", 2), new GenreCount("Drama", 1)));
        when(repo.mostExpensiveBooks(uid)).thenReturn(List.of(
            new BookPrice("Dune", new BigDecimal("20.00")), new BookPrice("1984", new BigDecimal("20.00"))));

        InsightResponse result = service.insights(uid);

        assertThat(result.sentences()).contains("Your most expensive books are 'Dune' and '1984', each at 20.00.");
    }

    @Test
    void tiedTopGenreStatTileJoinsEveryTiedGenreSoItDoesNotContradictTheSentence() {
        when(repo.counts(uid)).thenReturn(new Counts(6, 2, 2, 2));
        when(repo.averages(uid)).thenReturn(new Averages(new BigDecimal("12.50"), 300.0));
        when(repo.genreCounts(uid)).thenReturn(List.of(
            new GenreCount("Drama", 2), new GenreCount("Fantasy", 2), new GenreCount("Sci-Fi", 2)));
        when(repo.mostExpensiveBooks(uid)).thenReturn(List.of());

        InsightResponse result = service.insights(uid);

        assertThat(result.stats().topGenre()).isEqualTo("Drama, Fantasy, Sci-Fi");
    }

    @Test
    void summaryTemplateSkipsTheRedundantFinishedSentence() {
        when(repo.counts(uid)).thenReturn(new Counts(3, 1, 1, 1));
        when(repo.averages(uid)).thenReturn(new Averages(new BigDecimal("12.50"), 180.0));
        when(repo.genreCounts(uid)).thenReturn(List.of(new GenreCount("Fantasy", 2)));
        when(repo.mostExpensiveBooks(uid)).thenReturn(List.of(new BookPrice("Dune", new BigDecimal("20.00"))));

        String summary = service.summary(uid);

        assertThat(summary).isEqualTo(
            "You own 3 books, with 1 finished, 1 currently reading, and 1 still to read."
                + " Fantasy is your most read genre (2 books)."
                + " You tend to read short books (average 180 pages)."
                + " Your most expensive book is 'Dune' at 20.00.");
    }
}
