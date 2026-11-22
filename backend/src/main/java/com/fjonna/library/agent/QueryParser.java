package com.fjonna.library.agent;

import com.fjonna.library.agent.QuerySpec.Direction;
import com.fjonna.library.agent.QuerySpec.Filters;
import com.fjonna.library.book.BookStatus;
import java.math.BigDecimal;
import java.util.Collection;
import org.springframework.stereotype.Component;

/**
 * Legacy-compatible facade over {@link SpecParser}: parses a question into a {@link QuerySpec}
 * (see {@link #toSpec}), then projects that onto one of the original fixed {@link Intent} values
 * when the question matches an old, already-supported pattern. When it does not - a new slot
 * combination {@link Intent} has no case for (e.g. SUM(pages), a per-user ranking by pages, "the
 * 3 shortest books") - the derived intent is {@link Intent#UNKNOWN} and {@link AgentService} falls
 * through to the general engine, re-using the same {@link QuerySpec} via {@link #toSpec}.
 */
@Component
public class QueryParser {

    public record Parsed(Intent intent, Integer limit, String genre, String author, BookStatus status,
                          BigDecimal price, String searchText) {
    }

    private final SpecParser specParser = new SpecParser();

    public Parsed parse(String question, Collection<String> genres, Collection<String> authors) {
        QuerySpec spec = specParser.parse(question, genres, authors);
        Intent intent = deriveIntent(question, spec);
        BigDecimal price = intent == Intent.BOOKS_UNDER_PRICE ? spec.filters().priceMax() : null;
        String searchText = intent == Intent.SEARCH_TITLE ? spec.filters().titleContains() : null;
        return new Parsed(intent, spec.limit(), spec.filters().genre(), spec.filters().author(),
            spec.filters().status(), price, searchText);
    }

    /** The full parse, for {@link AgentService}'s generic engine once {@link #parse} says UNKNOWN. */
    public QuerySpec toSpec(String question, Collection<String> genres, Collection<String> authors) {
        return specParser.parse(question, genres, authors);
    }

    private Intent deriveIntent(String question, QuerySpec spec) {
        if (specParser.isPopularQuestion(question)) {
            return Intent.MOST_POPULAR_BOOK;
        }

        if (spec.groupBy() == GroupBy.USER) {
            boolean countOnly = spec.field() == null;
            if (countOnly && specParser.isOwnerRankingPhrase(question)) return Intent.MOST_BOOKS_OWNER;
            return Intent.UNKNOWN;
        }
        if (spec.groupBy() == GroupBy.GENRE) return Intent.GENRE_BREAKDOWN;
        if (spec.groupBy() == GroupBy.AUTHOR) return Intent.TOP_AUTHOR;

        if (spec.filters().titleContains() != null) {
            return Intent.SEARCH_TITLE;
        }

        if (spec.aggregate() == Aggregate.LIST && spec.orderBy() != null) {
            Field f = spec.orderBy().field();
            Direction d = spec.orderBy().direction();
            if (f == Field.PRICE && d == Direction.DESC) return Intent.TOP_EXPENSIVE;
            if (f == Field.PRICE && d == Direction.ASC) return Intent.CHEAPEST;
            if (f == Field.PAGES && d == Direction.DESC) return Intent.LONGEST;
            if (f == Field.CREATED_AT && d == Direction.DESC && specParser.isRecentPhrase(question)) {
                return Intent.RECENT_BOOKS;
            }
            return Intent.UNKNOWN;
        }

        if (spec.aggregate() == Aggregate.SUM && spec.field() == Field.PRICE) return Intent.TOTAL_VALUE;
        if (spec.aggregate() == Aggregate.AVG && spec.field() == Field.PRICE) return Intent.AVERAGE_PRICE;
        if (spec.aggregate() == Aggregate.COUNT) return Intent.COUNT_BOOKS;

        if (spec.aggregate() == Aggregate.LIST) {
            Filters f = spec.filters();
            boolean rangeBeyondLegacy = f.pagesMin() != null || f.pagesMax() != null || f.priceMin() != null;
            if (rangeBeyondLegacy) return Intent.UNKNOWN;
            if (f.priceMax() != null) return Intent.BOOKS_UNDER_PRICE;
            if (f.genre() != null || f.author() != null || f.status() != null
                || specParser.hasGenericBookCue(question)) {
                return Intent.LIST_BOOKS;
            }
            return Intent.UNKNOWN;
        }

        return Intent.UNKNOWN;
    }
}
