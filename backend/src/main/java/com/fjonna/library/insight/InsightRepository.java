package com.fjonna.library.insight;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Fixed, parameterized queries only. Pure aggregates: no SQL built from user input. */
@Repository
@RequiredArgsConstructor
public class InsightRepository {

    private final JdbcClient jdbc;

    public record Counts(long total, long finished, long reading, long toRead) {
    }

    public record Averages(BigDecimal avgPrice, Double avgPages) {
    }

    public record GenreCount(String genre, long count) {
    }

    public record BookPrice(String title, BigDecimal price) {
    }

    /** Always returns exactly one row: aggregates over zero rows yield zeros, not an empty result set. */
    public Counts counts(UUID uid) {
        return jdbc.sql("select count(*) as total,"
                + " count(*) filter (where status = 'COMPLETED') as finished,"
                + " count(*) filter (where status = 'READING') as reading,"
                + " count(*) filter (where status = 'TO_READ') as toRead"
                + " from books where user_id = :uid")
            .param("uid", uid)
            .query(Counts.class).single();
    }

    public Averages averages(UUID uid) {
        return jdbc.sql("select round(avg(price), 2) as avgPrice, avg(pages) as avgPages"
                + " from books where user_id = :uid")
            .param("uid", uid)
            .query(Averages.class).single();
    }

    /** Ordered by count desc, genre asc so ties resolve to the alphabetically first genre. */
    public List<GenreCount> genreCounts(UUID uid) {
        return jdbc.sql("select genre, count(*) as count from books"
                + " where user_id = :uid and genre is not null"
                + " group by genre order by count desc, genre")
            .param("uid", uid)
            .query(GenreCount.class).list();
    }

    /** Every book tied at the highest price (empty if none are priced), ordered by title. */
    public List<BookPrice> mostExpensiveBooks(UUID uid) {
        return jdbc.sql("select title, price from books"
                + " where user_id = :uid and price is not null"
                + " and price = (select max(price) from books where user_id = :uid and price is not null)"
                + " order by title")
            .param("uid", uid)
            .query(BookPrice.class).list();
    }
}
