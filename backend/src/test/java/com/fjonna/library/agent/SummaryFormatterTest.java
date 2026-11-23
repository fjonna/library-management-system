package com.fjonna.library.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.fjonna.library.agent.AgentRepository.Table;
import com.fjonna.library.agent.QuerySpec.Filters;
import com.fjonna.library.book.BookStatus;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class SummaryFormatterTest {

    private final SummaryFormatter fmt = new SummaryFormatter();

    @Test
    void scalarSummaryFormatsPriceWithCurrencyAndPluralizesCount() {
        QuerySpec spec = new QuerySpec(Aggregate.SUM, Field.PRICE, Filters.NONE, null, null, null);
        String summary = fmt.scalarSummary(spec, false, false, new BigDecimal("41.50"), 3);
        assertThat(summary).contains("$41.50").contains("3 books");
    }

    @Test
    void scalarSummarySingularForOneBook() {
        QuerySpec spec = new QuerySpec(Aggregate.AVG, Field.PAGES, Filters.NONE, null, null, null);
        String summary = fmt.scalarSummary(spec, false, false, 320, 1);
        assertThat(summary).contains("1 book").doesNotContain("1 books");
    }

    @Test
    void scalarSummaryIsFriendlyWhenNoDataAndNeverLeaksNull() {
        QuerySpec spec = new QuerySpec(Aggregate.AVG, Field.PRICE, Filters.NONE, null, null, null);
        String summary = fmt.scalarSummary(spec, false, false, BigDecimal.ZERO, 0);
        assertThat(summary).doesNotContainIgnoringCase("null");
        assertThat(summary).contains("don't have any books with a price");
    }

    @Test
    void scalarSummaryNeverShowsRawFilterKeysAndIsBilingual() {
        Filters f = new Filters("fantasy", null, null, null, null, null, null, null);
        QuerySpec spec = new QuerySpec(Aggregate.AVG, Field.PAGES, f, null, null, null);

        String english = fmt.scalarSummary(spec, false, false, 320, 2);
        assertThat(english).contains("Fantasy").doesNotContain("genre=");

        String albanian = fmt.scalarSummary(spec, false, true, 320, 2);
        assertThat(albanian).contains("Fantasy").doesNotContain("genre=").doesNotContainIgnoringCase("average");
    }

    @Test
    void noMatchMessageDescribesActiveFiltersInPlainLanguage() {
        Filters f = new Filters("fantasy", null, null, null, null, null, 300, null);
        QuerySpec spec = new QuerySpec(Aggregate.LIST, null, f, null, null, null);
        String msg = fmt.noMatchMessage(spec, false, false);
        assertThat(msg).contains("Fantasy").contains("300").contains("pages").doesNotContain("genre=").doesNotContain("pages>");
    }

    @Test
    void noMatchMessageWithNoFiltersIsGeneric() {
        QuerySpec spec = new QuerySpec(Aggregate.LIST, null, Filters.NONE, null, null, null);
        assertThat(fmt.noMatchMessage(spec, true, false)).isEqualTo("No books found in the whole library.");
    }

    @Test
    void groupSummaryNamesTopGroupAndValue() {
        QuerySpec spec = new QuerySpec(Aggregate.GROUP_COUNT, null, Filters.NONE, GroupBy.USER, null, null);
        Table t = new Table(List.of("User", "Books"), List.of(List.of("alice", 5L), List.of("bob", 2L)));
        assertThat(fmt.groupSummary(spec, true, false, t)).contains("alice").contains("5");
    }

    @Test
    void understoodAsListsAggregateFieldAndFilters() {
        Filters f = new Filters("fantasy", null, null, null, null, null, null, null);
        QuerySpec spec = new QuerySpec(Aggregate.AVG, Field.PAGES, f, null, null, null);
        String understood = fmt.understoodAs(spec, false);
        assertThat(understood).containsIgnoringCase("average").contains("pages").contains("genre=fantasy");
    }

    @Test
    void statusLabelIsFriendlyInBothLanguages() {
        assertThat(SummaryFormatter.statusLabel(BookStatus.COMPLETED, false)).isEqualTo("completed");
        assertThat(SummaryFormatter.statusLabel(BookStatus.READING, false)).isEqualTo("currently reading");
        assertThat(SummaryFormatter.statusLabel(BookStatus.TO_READ, false)).isEqualTo("to read");
        assertThat(SummaryFormatter.statusLabel(BookStatus.COMPLETED, true)).isEqualTo("të përfunduara");
        assertThat(SummaryFormatter.statusLabel(BookStatus.READING, true)).isEqualTo("duke i lexuar");
        assertThat(SummaryFormatter.statusLabel(BookStatus.TO_READ, true)).isEqualTo("për t'u lexuar");
    }
}
