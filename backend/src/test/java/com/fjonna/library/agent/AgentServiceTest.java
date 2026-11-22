package com.fjonna.library.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fjonna.library.agent.AgentRepository.Table;
import com.fjonna.library.agent.QueryParser.Parsed;
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
        assertThat(result.summary()).contains("12.50").contains("2 priced book(s)");
    }

    @Test
    void topAuthorWithNoBooksDoesNotThrowAndIsFriendly() {
        stubParse(Intent.TOP_AUTHOR);
        when(repo.topAuthor(any(), anyInt())).thenReturn(new Table(List.of("Author", "Books"), List.of()));

        QueryResult result = service.answer(uid, false, "Which author has the most books?");

        assertThat(result.summary()).doesNotContainIgnoringCase("null");
        assertThat(result.summary()).contains("No books found");
    }
}
