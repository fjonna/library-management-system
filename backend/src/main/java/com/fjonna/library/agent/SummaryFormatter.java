package com.fjonna.library.agent;

import com.fjonna.library.agent.AgentRepository.Table;
import com.fjonna.library.agent.QuerySpec.Filters;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Builds the human-readable parts of a {@link QueryResult} (title, "understood as" sentence,
 * summary, empty-result message) for a {@link QuerySpec} answered by the generic engine.
 */
public class SummaryFormatter {

    private static final NumberFormat INTEGER_FORMAT = NumberFormat.getIntegerInstance(Locale.US);

    public String label(QuerySpec spec) {
        StringBuilder sb = new StringBuilder();
        if (spec.groupBy() != null) {
            sb.append(spec.field() == null ? "count" : aggregateWord(spec.aggregate()))
                .append(" per ").append(spec.groupBy().name().toLowerCase(Locale.ROOT));
        } else {
            sb.append(aggregateWord(spec.aggregate()));
            if (spec.field() != null) sb.append(" of ").append(fieldWord(spec.field()));
        }
        return sb.toString();
    }

    public String title(QuerySpec spec) {
        String base = switch (spec.aggregate()) {
            case COUNT -> "Book count";
            case SUM -> "Total " + fieldWord(spec.field());
            case AVG -> "Average " + fieldWord(spec.field());
            case MIN -> "Lowest " + fieldWord(spec.field());
            case MAX -> "Highest " + fieldWord(spec.field());
            case LIST -> "Books";
            case GROUP_COUNT -> "Books per " + groupWord(spec.groupBy());
        };
        if (spec.groupBy() != null && spec.field() != null) {
            base = fieldWord(spec.field()) + " per " + groupWord(spec.groupBy());
        }
        return capitalize(base);
    }

    public String understoodAs(QuerySpec spec, boolean admin) {
        StringBuilder sb = new StringBuilder();
        if (spec.groupBy() == GroupBy.USER) {
            sb.append(spec.field() == null ? "Book count" : capitalize(aggregateWord(spec.aggregate()) + " of " + fieldWord(spec.field())))
                .append(" grouped by user (library-wide)");
        } else if (spec.groupBy() != null) {
            sb.append("Book count grouped by ").append(groupWord(spec.groupBy()))
                .append(" (").append(admin ? "library-wide" : "in your books").append(")");
        } else {
            sb.append(capitalize(aggregateWord(spec.aggregate())));
            if (spec.field() != null) sb.append(" of ").append(fieldWord(spec.field()));
            if (spec.orderBy() != null) {
                sb.append(", ordered by ").append(fieldWord(spec.orderBy().field()))
                    .append(spec.orderBy().direction() == QuerySpec.Direction.ASC ? " ascending" : " descending");
            }
            sb.append(" (").append(admin ? "library-wide" : "in your books").append(")");
        }
        appendFilters(sb, spec.filters());
        if (spec.limit() != null) sb.append(", limit=").append(spec.limit());
        return sb.toString();
    }

    private void appendFilters(StringBuilder sb, Filters f) {
        if (f.genre() != null) sb.append(", genre=").append(f.genre());
        if (f.author() != null) sb.append(", author=").append(f.author());
        if (f.status() != null) sb.append(", status=").append(f.status());
        if (f.titleContains() != null) sb.append(", title~").append(f.titleContains());
        if (f.priceMin() != null) sb.append(", price>=").append(f.priceMin());
        if (f.priceMax() != null) sb.append(", price<=").append(f.priceMax());
        if (f.pagesMin() != null) sb.append(", pages>=").append(f.pagesMin());
        if (f.pagesMax() != null) sb.append(", pages<=").append(f.pagesMax());
    }

    /** Short "no results" sentence listing the active filters, e.g. "No books match: genre=fantasy, pages>300." */
    public String noMatchMessage(QuerySpec spec, boolean admin) {
        Filters f = spec.filters();
        StringBuilder desc = new StringBuilder();
        if (f.genre() != null) appendCsv(desc, "genre=" + f.genre());
        if (f.author() != null) appendCsv(desc, "author=" + f.author());
        if (f.status() != null) appendCsv(desc, "status=" + f.status());
        if (f.titleContains() != null) appendCsv(desc, "title~\"" + f.titleContains() + "\"");
        if (f.priceMin() != null) appendCsv(desc, "price>=" + f.priceMin());
        if (f.priceMax() != null) appendCsv(desc, "price<=" + f.priceMax());
        if (f.pagesMin() != null) appendCsv(desc, "pages>" + f.pagesMin());
        if (f.pagesMax() != null) appendCsv(desc, "pages<" + f.pagesMax());
        String where = admin ? "in the whole library" : "in your library";
        return desc.isEmpty()
            ? "No books found " + where + "."
            : "No books match " + where + ": " + desc + ".";
    }

    private static void appendCsv(StringBuilder sb, String piece) {
        if (!sb.isEmpty()) sb.append(", ");
        sb.append(piece);
    }

    /** Summary sentence for a single-row scalar aggregate result: value + count, e.g. "3,450 pages across 12 book(s)." */
    public String scalarSummary(QuerySpec spec, boolean admin, Object value, long count) {
        String where = admin ? "in the whole library" : "in your library";
        if (count == 0) {
            return "You don't have any books with a " + fieldWord(spec.field()) + " recorded yet." + filterSuffix(spec);
        }
        String formattedValue = formatValue(spec.field(), value);
        String verb = switch (spec.aggregate()) {
            case SUM -> "Total";
            case AVG -> "Average";
            case MIN -> "Lowest";
            case MAX -> "Highest";
            default -> "Value";
        };
        return verb + " " + fieldWord(spec.field()) + " " + where + ": " + formattedValue
            + " across " + pluralize(count, "book") + "." + filterSuffix(spec);
    }

    public String countSummary(QuerySpec spec, boolean admin, long count) {
        String where = admin ? "in the whole library" : "in your library";
        return pluralize(count, "book") + " " + where + "." + filterSuffix(spec);
    }

    public String groupSummary(QuerySpec spec, boolean admin, Table t) {
        String where = admin ? "in the whole library" : "in your library";
        if (t.rows().isEmpty()) return "No data to group " + where + "." + filterSuffix(spec);
        Object topGroup = t.rows().get(0).get(0);
        Object topValue = t.rows().get(0).get(1);
        String what = spec.field() == null ? "books" : fieldWord(spec.field());
        return topGroup + " has the most " + what + " (" + formatValue(spec.field(), topValue) + ") among "
            + pluralize(t.rows().size(), groupWord(spec.groupBy()).toLowerCase(Locale.ROOT)) + "." + filterSuffix(spec);
    }

    public String listSummary(QuerySpec spec, boolean admin, Table t) {
        String where = admin ? "in the whole library" : "in your library";
        if (t.rows().isEmpty()) return noMatchMessage(spec, admin);
        if (spec.orderBy() != null) {
            String title = String.valueOf(t.rows().get(0).get(1));
            String verb = spec.orderBy().direction() == QuerySpec.Direction.ASC ? "lowest" : "highest";
            return "The book with the " + verb + " " + fieldWord(spec.orderBy().field()) + " " + where
                + " is '" + title + "'." + filterSuffix(spec);
        }
        return "Found " + pluralize(t.rows().size(), "book") + " " + where + "." + filterSuffix(spec);
    }

    private String filterSuffix(QuerySpec spec) {
        StringBuilder sb = new StringBuilder();
        appendFilters(sb, spec.filters());
        return sb.isEmpty() ? "" : " Filters:" + sb + ".";
    }

    private static String formatValue(Field field, Object value) {
        if (value == null) return "0";
        if (field == Field.PRICE) {
            BigDecimal d = value instanceof BigDecimal bd ? bd : new BigDecimal(value.toString());
            return "$" + d.setScale(2, java.math.RoundingMode.HALF_UP);
        }
        if (value instanceof Number n) return INTEGER_FORMAT.format(n.longValue());
        return String.valueOf(value);
    }

    private static String pluralize(long count, String noun) {
        return INTEGER_FORMAT.format(count) + " " + noun + (count == 1 ? "" : "s");
    }

    private static String aggregateWord(Aggregate aggregate) {
        return switch (aggregate) {
            case COUNT, GROUP_COUNT -> "count";
            case SUM -> "total";
            case AVG -> "average";
            case MIN -> "lowest";
            case MAX -> "highest";
            case LIST -> "list";
        };
    }

    private static String fieldWord(Field field) {
        if (field == null) return "books";
        return switch (field) {
            case PRICE -> "price";
            case PAGES -> "pages";
            case TITLE -> "title";
            case AUTHOR -> "author";
            case GENRE -> "genre";
            case STATUS -> "status";
            case CREATED_AT -> "date added";
        };
    }

    private static String groupWord(GroupBy groupBy) {
        return switch (groupBy) {
            case GENRE -> "genre";
            case AUTHOR -> "author";
            case STATUS -> "status";
            case USER -> "user";
        };
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }
}
