package com.fjonna.library.agent;

import com.fjonna.library.book.BookStatus;
import java.math.BigDecimal;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Fixed, parameterized queries only. User text is never concatenated into SQL. */
@Repository
@RequiredArgsConstructor
public class AgentRepository {

    private final JdbcClient jdbc;

    public record Table(List<String> columns, List<List<Object>> rows) {
    }

    /** scope == null means "whole library" (admin); otherwise only that user's books. */
    public record Filter(UUID scope, String genre, String author, BookStatus status) {
    }

    public enum Sort {
        NEWEST("b.created_at desc", ""),
        PRICE_DESC("b.price desc", " and b.price is not null"),
        PRICE_ASC("b.price asc", " and b.price is not null"),
        PAGES_DESC("b.pages desc", " and b.pages is not null");

        final String orderBy;
        final String condition;

        Sort(String orderBy, String condition) {
            this.orderBy = orderBy;
            this.condition = condition;
        }
    }

    private record Where(String sql, Map<String, Object> params) {
    }

    /** Runs a {@link SqlBuilder.BuiltSql}: the fixed, parameterized SQL built by {@link SqlBuilder} for a {@link QuerySpec}. */
    public Table run(SqlBuilder.BuiltSql built) {
        return run(built.sql(), built.params());
    }

    public List<String> genres(UUID scope) {
        return distinct("genre", scope);
    }

    public List<String> authors(UUID scope) {
        return distinct("author", scope);
    }

    private List<String> distinct(String column, UUID scope) {
        // column is one of two constants from this class, never user input
        String sql = "select distinct lower(" + column + ") from books where " + column + " is not null"
            + (scope != null ? " and user_id = :uid" : "");
        JdbcClient.StatementSpec spec = jdbc.sql(sql);
        if (scope != null) spec = spec.param("uid", scope);
        return spec.query(String.class).list();
    }

    public Table books(Filter f, boolean admin, Sort sort, int limit) {
        Where w = where(f);
        String sql = "select b.id as \"ID\", b.title as \"Title\", b.author as \"Author\", b.genre as \"Genre\","
            + " b.status as \"Status\", b.price as \"Price\", b.pages as \"Pages\""
            + (admin ? ", u.username as \"Owner\"" : "")
            + " from books b join users u on u.id = b.user_id"
            + w.sql() + sort.condition
            + " order by " + sort.orderBy + ", b.id limit :lim";
        w.params().put("lim", limit);
        return run(sql, w.params());
    }

    public Table count(Filter f) {
        Where w = where(f);
        return run("select count(*) as \"Books\" from books b join users u on u.id = b.user_id" + w.sql(), w.params());
    }

    public Table totalValue(Filter f) {
        Where w = where(f);
        return run("select coalesce(sum(b.price), 0) as \"Total value\", count(*) as \"Books\""
            + " from books b join users u on u.id = b.user_id" + w.sql(), w.params());
    }

    public Table genreBreakdown(Filter f) {
        Where w = where(f);
        return run("select coalesce(b.genre, 'Unknown') as \"Genre\", count(*) as \"Books\""
            + " from books b join users u on u.id = b.user_id" + w.sql()
            + " group by 1 order by 2 desc, 1", w.params());
    }

    public Table booksUnderPrice(Filter f, boolean admin, BigDecimal maxPrice, int limit) {
        Where w = where(f);
        String sql = "select b.id as \"ID\", b.title as \"Title\", b.author as \"Author\", b.genre as \"Genre\","
            + " b.status as \"Status\", b.price as \"Price\", b.pages as \"Pages\""
            + (admin ? ", u.username as \"Owner\"" : "")
            + " from books b join users u on u.id = b.user_id"
            + w.sql() + " and b.price is not null and b.price <= :maxPrice"
            + " order by b.price asc, b.id limit :lim";
        w.params().put("maxPrice", maxPrice);
        w.params().put("lim", limit);
        return run(sql, w.params());
    }

    /** coalesce(...,0): avg() over zero priced books is SQL NULL, never surfaced to the caller. */
    public Table averagePrice(Filter f) {
        Where w = where(f);
        return run("select coalesce(round(avg(b.price), 2), 0) as \"Average price\", count(b.price) as \"Priced books\""
            + " from books b join users u on u.id = b.user_id" + w.sql() + " and b.price is not null", w.params());
    }

    public Table topAuthor(Filter f, int limit) {
        Where w = where(f);
        String sql = "select b.author as \"Author\", count(*) as \"Books\""
            + " from books b join users u on u.id = b.user_id" + w.sql()
            + " group by b.author order by 2 desc, 1 limit :lim";
        w.params().put("lim", limit);
        return run(sql, w.params());
    }

    public Table searchTitle(Filter f, boolean admin, String text, int limit) {
        Where w = where(f);
        String pattern = "%" + text.toLowerCase(Locale.ROOT)
            .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        String sql = "select b.id as \"ID\", b.title as \"Title\", b.author as \"Author\", b.genre as \"Genre\","
            + " b.status as \"Status\", b.price as \"Price\", b.pages as \"Pages\""
            + (admin ? ", u.username as \"Owner\"" : "")
            + " from books b join users u on u.id = b.user_id"
            + w.sql() + " and lower(b.title) like :pattern"
            + " order by b.created_at desc, b.id limit :lim";
        w.params().put("pattern", pattern);
        w.params().put("lim", limit);
        return run(sql, w.params());
    }

    /** Admin only (enforced in AgentService). */
    public Table topOwners(int limit) {
        return run("select u.username as \"User\", count(b.id) as \"Books\""
            + " from users u left join books b on b.user_id = u.id"
            + " group by u.username order by 2 desc, 1 limit :lim", Map.of("lim", limit));
    }

    /** "Popular" = same title and author owned by the most different users. Aggregates only, no owner names. */
    public Table popularBooks(int limit) {
        return run("select min(b.title) as \"Title\", min(b.author) as \"Author\","
            + " count(distinct b.user_id) as \"Owners\""
            + " from books b group by lower(trim(b.title)), lower(trim(b.author))"
            + " order by 3 desc, 1 limit :lim", Map.of("lim", limit));
    }

    private Where where(Filter f) {
        StringBuilder sb = new StringBuilder(" where 1=1");
        Map<String, Object> p = new HashMap<>();
        if (f.scope() != null) {
            sb.append(" and b.user_id = :uid");
            p.put("uid", f.scope());
        }
        if (f.genre() != null) {
            sb.append(" and lower(b.genre) = :genre");
            p.put("genre", f.genre());
        }
        if (f.author() != null) {
            sb.append(" and lower(b.author) = :author");
            p.put("author", f.author());
        }
        if (f.status() != null) {
            sb.append(" and b.status = :status");
            p.put("status", f.status().name());
        }
        return new Where(sb.toString(), p);
    }

    private Table run(String sql, Map<String, Object> params) {
        return jdbc.sql(sql).params(params).query(rs -> {
            ResultSetMetaData md = rs.getMetaData();
            List<String> cols = new ArrayList<>();
            for (int i = 1; i <= md.getColumnCount(); i++) cols.add(md.getColumnLabel(i));
            List<List<Object>> rows = new ArrayList<>();
            while (rs.next()) {
                List<Object> row = new ArrayList<>();
                for (int i = 1; i <= cols.size(); i++) row.add(rs.getObject(i));
                rows.add(row);
            }
            return new Table(cols, rows);
        });
    }
}
