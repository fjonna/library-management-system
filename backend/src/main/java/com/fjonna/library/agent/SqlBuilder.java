package com.fjonna.library.agent;

import com.fjonna.library.agent.QuerySpec.Direction;
import com.fjonna.library.agent.QuerySpec.Filters;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Turns a {@link QuerySpec} into fixed, parameterized SQL. {@link Field} and {@link GroupBy} values
 * only ever resolve to a column expression from the whitelists below - they are never built from a
 * string the user typed. Everything that DOES come from user text (genre, author, title, status,
 * price/page thresholds) is placed into the params map and bound by name; it never touches the SQL
 * string. As a result, the SQL text produced for a given spec "shape" is identical no matter what
 * values the filters carry - see {@code SqlBuilderInjectionTest}.
 */
@Component
public class SqlBuilder {

    /**
     * {@code displayLimit}/{@code tieColumn} let {@link AgentRepository#run(BuiltSql)} fetch a
     * generous buffer (see {@link #DEFAULT_LIMIT}) but still show only {@code displayLimit} rows -
     * plus any further rows tied with the one at that cutoff on column {@code tieColumn} (-1 means
     * "no tie column to check", e.g. a plain count/scalar result with nothing to trim).
     */
    public record BuiltSql(String sql, Map<String, Object> params, int displayLimit, int tieColumn) {
        public BuiltSql(String sql, Map<String, Object> params) {
            this(sql, params, Integer.MAX_VALUE, -1);
        }
    }

    private static final Map<Field, String> COLUMN = Map.of(
        Field.PRICE, "b.price",
        Field.PAGES, "b.pages",
        Field.TITLE, "b.title",
        Field.AUTHOR, "b.author",
        Field.GENRE, "b.genre",
        Field.STATUS, "b.status",
        Field.CREATED_AT, "b.created_at");

    private static final Map<Field, String> FIELD_LABEL = Map.of(
        Field.PRICE, "Price", Field.PAGES, "Pages", Field.TITLE, "Title", Field.AUTHOR, "Author",
        Field.GENRE, "Genre", Field.STATUS, "Status", Field.CREATED_AT, "Added");

    private static final Map<GroupBy, String> GROUP_COLUMN = Map.of(
        GroupBy.GENRE, "coalesce(b.genre, 'Unknown')",
        GroupBy.AUTHOR, "b.author",
        GroupBy.STATUS, "b.status",
        GroupBy.USER, "u.username");

    private static final Map<GroupBy, String> GROUP_LABEL = Map.of(
        GroupBy.GENRE, "Genre", GroupBy.AUTHOR, "Author", GroupBy.STATUS, "Status", GroupBy.USER, "User");

    private static final int DEFAULT_LIMIT = 50;

    public BuiltSql build(QuerySpec spec, UUID scope, boolean admin) {
        Map<String, Object> params = new LinkedHashMap<>();
        String where = whereClause(spec.filters(), scope, params);

        if (spec.groupBy() != null) {
            return buildGrouped(spec, where, params);
        }
        return switch (spec.aggregate()) {
            case LIST -> buildList(spec, admin, where, params);
            case COUNT -> buildCount(where, params);
            case SUM, AVG, MIN, MAX -> buildScalarAggregate(spec, where, params);
            case GROUP_COUNT -> throw new IllegalArgumentException("GROUP_COUNT requires a groupBy");
        };
    }

    private BuiltSql buildCount(String where, Map<String, Object> params) {
        String sql = "select count(*) as \"Count\" from books b join users u on u.id = b.user_id" + where;
        return new BuiltSql(sql, params);
    }

    private BuiltSql buildScalarAggregate(QuerySpec spec, String where, Map<String, Object> params) {
        String col = COLUMN.get(spec.field());
        String fn = aggregateFunction(spec.aggregate());
        String sql = "select coalesce(" + fn + "(" + col + "), 0) as \"Value\", count(" + col + ") as \"Count\""
            + " from books b join users u on u.id = b.user_id" + where + " and " + col + " is not null";
        return new BuiltSql(sql, params);
    }

    /** Position of {@code field}'s column in {@link #buildList}'s fixed select list, or -1 if not selected. */
    private static int listTieColumn(Field field) {
        return switch (field) {
            case TITLE -> 1;
            case AUTHOR -> 2;
            case GENRE -> 3;
            case STATUS -> 4;
            case PRICE -> 5;
            case PAGES -> 6;
            case CREATED_AT -> -1;
        };
    }

    private BuiltSql buildList(QuerySpec spec, boolean admin, String where, Map<String, Object> params) {
        String ownerCol = admin ? ", u.username as \"Owner\"" : "";
        String orderSql;
        int tieColumn;
        if (spec.orderBy() != null) {
            String col = COLUMN.get(spec.orderBy().field());
            orderSql = col + " " + direction(spec.orderBy().direction()) + " nulls last, b.title, b.id";
            tieColumn = listTieColumn(spec.orderBy().field());
        } else {
            orderSql = "b.created_at desc, b.title, b.id";
            tieColumn = -1;
        }
        int displayLimit = spec.limit() == null ? DEFAULT_LIMIT : spec.limit();
        params.put("lim", DEFAULT_LIMIT);
        String sql = "select b.id as \"ID\", b.title as \"Title\", b.author as \"Author\", b.genre as \"Genre\","
            + " b.status as \"Status\", b.price as \"Price\", b.pages as \"Pages\"" + ownerCol
            + " from books b join users u on u.id = b.user_id" + where
            + " order by " + orderSql + " limit :lim";
        return new BuiltSql(sql, params, displayLimit, tieColumn);
    }

    private BuiltSql buildGrouped(QuerySpec spec, String where, Map<String, Object> params) {
        GroupBy groupBy = spec.groupBy();
        String groupCol = GROUP_COLUMN.get(groupBy);
        String groupLabel = GROUP_LABEL.get(groupBy);
        boolean byUser = groupBy == GroupBy.USER;
        String from = byUser
            ? "users u left join books b on b.user_id = u.id"
            : "books b join users u on u.id = b.user_id";

        boolean countOnly = spec.aggregate() == Aggregate.COUNT || spec.aggregate() == Aggregate.GROUP_COUNT;
        String valueExpr;
        String valueLabel;
        if (countOnly) {
            valueExpr = byUser ? "count(b.id)" : "count(*)";
            valueLabel = "Books";
        } else {
            String col = COLUMN.get(spec.field());
            valueExpr = "coalesce(" + aggregateFunction(spec.aggregate()) + "(" + col + "), 0)";
            valueLabel = FIELD_LABEL.get(spec.field());
        }
        Direction dir = spec.orderBy() != null ? spec.orderBy().direction() : Direction.DESC;
        int displayLimit = spec.limit() == null ? DEFAULT_LIMIT : spec.limit();
        params.put("lim", DEFAULT_LIMIT);

        String sql = "select " + groupCol + " as \"" + groupLabel + "\", " + valueExpr + " as \"" + valueLabel + "\""
            + " from " + from + where
            + " group by 1 order by 2 " + direction(dir) + ", 1 limit :lim";
        return new BuiltSql(sql, params, displayLimit, 1);
    }

    private static String aggregateFunction(Aggregate aggregate) {
        return switch (aggregate) {
            case SUM -> "sum";
            case AVG -> "avg";
            case MIN -> "min";
            case MAX -> "max";
            default -> throw new IllegalArgumentException("Not a scalar aggregate: " + aggregate);
        };
    }

    private static String direction(Direction direction) {
        return direction == Direction.ASC ? "asc" : "desc";
    }

    private String whereClause(Filters f, UUID scope, Map<String, Object> params) {
        StringBuilder sb = new StringBuilder(" where 1=1");
        if (scope != null) {
            sb.append(" and b.user_id = :uid");
            params.put("uid", scope);
        }
        if (f.genre() != null) {
            sb.append(" and lower(b.genre) = :genre");
            params.put("genre", f.genre());
        }
        if (f.author() != null) {
            sb.append(" and lower(b.author) = :author");
            params.put("author", f.author());
        }
        if (f.status() != null) {
            sb.append(" and b.status = :status");
            params.put("status", f.status().name());
        }
        if (f.titleContains() != null) {
            sb.append(" and lower(b.title) like :titlePattern");
            params.put("titlePattern", likePattern(f.titleContains()));
        }
        if (f.priceMin() != null) {
            sb.append(" and b.price >= :priceMin");
            params.put("priceMin", f.priceMin());
        }
        if (f.priceMax() != null) {
            sb.append(" and b.price <= :priceMax");
            params.put("priceMax", f.priceMax());
        }
        if (f.pagesMin() != null) {
            sb.append(" and b.pages >= :pagesMin");
            params.put("pagesMin", f.pagesMin());
        }
        if (f.pagesMax() != null) {
            sb.append(" and b.pages <= :pagesMax");
            params.put("pagesMax", f.pagesMax());
        }
        return sb.toString();
    }

    private static String likePattern(String text) {
        return "%" + text.toLowerCase(Locale.ROOT)
            .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
