package com.fjonna.library.agent;

import com.fjonna.library.book.BookStatus;
import java.math.BigDecimal;

/**
 * A fully-parsed question: what to compute ({@link #aggregate()}/{@link #field()}), over which rows
 * ({@link #filters()}), optionally bucketed ({@link #groupBy()}), in what order, and how many rows.
 * Every slot is an enum or a plain value - never a piece of SQL. Only {@link SqlBuilder} knows how to
 * turn a spec into SQL, and it only ever maps enums to a fixed whitelist of column expressions; the
 * values inside {@link Filters} reach the database exclusively as bound parameters.
 */
public record QuerySpec(
    Aggregate aggregate,
    Field field,
    Filters filters,
    GroupBy groupBy,
    OrderBy orderBy,
    Integer limit) {

    public QuerySpec {
        if (filters == null) filters = Filters.NONE;
    }

    public record Filters(
        String genre,
        String author,
        BookStatus status,
        String titleContains,
        BigDecimal priceMin,
        BigDecimal priceMax,
        Integer pagesMin,
        Integer pagesMax) {

        public static final Filters NONE = new Filters(null, null, null, null, null, null, null, null);

        public boolean isEmpty() {
            return genre == null && author == null && status == null && titleContains == null
                && priceMin == null && priceMax == null && pagesMin == null && pagesMax == null;
        }
    }

    public record OrderBy(Field field, Direction direction) {
    }

    public enum Direction {
        ASC, DESC
    }
}
