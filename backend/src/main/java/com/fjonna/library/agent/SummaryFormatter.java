package com.fjonna.library.agent;

import com.fjonna.library.agent.AgentRepository.Table;
import com.fjonna.library.agent.QuerySpec.Filters;
import com.fjonna.library.book.BookStatus;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the human-readable parts of a {@link QueryResult} (title, "understood as" sentence,
 * summary, empty-result message) for a {@link QuerySpec} answered by the generic engine.
 *
 * <p>{@link #understoodAs} is a machine-readable-ish debug sentence kept verbatim in the API
 * response (the frontend reads "genre=", "author=" out of it to build follow-up chips) - it is
 * never shown to the user, so it still uses raw {@code key=value} pairs. Every other method here
 * builds the sentence the user actually reads, so filters are woven in as plain language instead,
 * in English or Albanian depending on {@code albanian} (see {@link Vocabulary#isAlbanian}).
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

    /** Kept verbatim (raw key=value filters) - part of the API contract, never rendered by the UI. */
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

    /**
     * Plain-language description of the active filters, e.g. "in Fantasy and by Tolkien", or ""
     * when there are none. Never shown with raw field names - this is what replaced the old
     * "Filters: genre=x, author=y." suffix everywhere below.
     */
    private String naturalFilters(Filters f, boolean albanian) {
        List<String> parts = new ArrayList<>();
        if (f.status() != null) parts.add(statusLabel(f.status(), albanian));
        if (f.genre() != null) parts.add((albanian ? "nga zhanri " : "in ") + capitalize(f.genre()));
        if (f.author() != null) parts.add((albanian ? "nga " : "by ") + titleCase(f.author()));
        if (f.titleContains() != null) {
            parts.add((albanian ? "me titullin \"" : "titled \"") + f.titleContains() + "\"");
        }
        if (f.priceMin() != null) parts.add((albanian ? "me çmim mbi $" : "priced over $") + f.priceMin());
        if (f.priceMax() != null) parts.add((albanian ? "me çmim nën $" : "priced under $") + f.priceMax());
        if (f.pagesMin() != null) parts.add((albanian ? "me mbi " : "over ") + f.pagesMin() + (albanian ? " faqe" : " pages"));
        if (f.pagesMax() != null) parts.add((albanian ? "me nën " : "under ") + f.pagesMax() + (albanian ? " faqe" : " pages"));
        return String.join(albanian ? " dhe " : " and ", parts);
    }

    /** "No results" sentence in plain language, e.g. "No books found in your library in Fantasy." */
    public String noMatchMessage(QuerySpec spec, boolean admin, boolean albanian) {
        String desc = naturalFilters(spec.filters(), albanian);
        if (albanian) {
            String who = admin ? "në bibliotekë" : "në librat e tu";
            return desc.isEmpty() ? "Nuk ka libra " + who + "." : "Nuk ka libra " + desc + " " + who + ".";
        }
        String where = admin ? "in the whole library" : "in your library";
        return desc.isEmpty() ? "No books found " + where + "." : "No books found " + where + " " + desc + ".";
    }

    /** Summary sentence for a single-row scalar aggregate result: value + count, e.g. "3,450 pages across 12 book(s)." */
    public String scalarSummary(QuerySpec spec, boolean admin, boolean albanian, Object value, long count) {
        String desc = naturalFilters(spec.filters(), albanian);
        if (count == 0) {
            if (albanian) {
                return desc.isEmpty()
                    ? "Nuk ke asnjë libër me " + fieldWordAl(spec.field()) + " të regjistruar."
                    : "Nuk ke asnjë libër " + desc + " me " + fieldWordAl(spec.field()) + " të regjistruar.";
            }
            return desc.isEmpty()
                ? "You don't have any books with a " + fieldWord(spec.field()) + " recorded yet."
                : "You don't have any books with a " + fieldWord(spec.field()) + " recorded yet, " + desc + ".";
        }
        String formattedValue = formatValue(spec.field(), value);
        if (albanian) {
            String verb = switch (spec.aggregate()) {
                case SUM -> "Totali i";
                case AVG -> "Mesatarja e";
                case MIN -> "Më i ulëti";
                case MAX -> "Më i larti";
                default -> "Vlera e";
            };
            String who = admin ? "në bibliotekë" : "në librat e tu";
            String scope = desc.isEmpty() ? who : desc + " " + who;
            return verb + " " + fieldWordAl(spec.field()) + " " + scope + ": " + formattedValue
                + " nga " + pluralizeAl(count, "libër", "libra") + ".";
        }
        String where = admin ? "in the whole library" : "in your library";
        String scope = desc.isEmpty() ? where : where + " " + desc;
        String verb = switch (spec.aggregate()) {
            case SUM -> "Total";
            case AVG -> "Average";
            case MIN -> "Lowest";
            case MAX -> "Highest";
            default -> "Value";
        };
        return verb + " " + fieldWord(spec.field()) + " " + scope + ": " + formattedValue
            + " across " + pluralize(count, "book") + ".";
    }

    public String countSummary(QuerySpec spec, boolean admin, boolean albanian, long count) {
        String desc = naturalFilters(spec.filters(), albanian);
        if (albanian) {
            if (count == 0) {
                String who = admin ? "në bibliotekë" : "";
                return desc.isEmpty()
                    ? (admin ? "Nuk ka asnjë libër " + who + "." : "Nuk ke asnjë libër.")
                    : (admin ? "Nuk ka asnjë libër " + desc + " " + who + "." : "Nuk ke asnjë libër " + desc + ".");
            }
            String noun = count == 1 ? "libër" : "libra";
            if (admin) {
                return "Ka " + INTEGER_FORMAT.format(count) + " " + noun
                    + (desc.isEmpty() ? "" : " " + desc) + " në bibliotekë.";
            }
            return "Ke " + INTEGER_FORMAT.format(count) + " " + noun + (desc.isEmpty() ? "" : " " + desc) + ".";
        }
        String where = admin ? "in the whole library" : "in your library";
        String scope = desc.isEmpty() ? where : where + " " + desc;
        return pluralize(count, "book") + " " + scope + ".";
    }

    public String groupSummary(QuerySpec spec, boolean admin, boolean albanian, Table t) {
        String desc = naturalFilters(spec.filters(), albanian);
        if (t.rows().isEmpty()) {
            if (albanian) {
                String who = admin ? "në bibliotekë" : "në librat e tu";
                return desc.isEmpty() ? "Nuk ka të dhëna " + who + "." : "Nuk ka të dhëna " + desc + " " + who + ".";
            }
            String where = admin ? "in the whole library" : "in your library";
            return desc.isEmpty() ? "No data to group " + where + "." : "No data to group " + where + " " + desc + ".";
        }
        Object topGroup = t.rows().get(0).get(0);
        Object topValue = t.rows().get(0).get(1);
        if (albanian) {
            String what = spec.field() == null ? "libra" : fieldWordAl(spec.field());
            return topGroup + " ka më shumë " + what + " (" + formatValue(spec.field(), topValue) + ") mes "
                + pluralizeAl(t.rows().size(), groupWordAl(spec.groupBy()), groupWordAl(spec.groupBy()) + "ve") + "."
                + (desc.isEmpty() ? "" : " (" + desc + ")");
        }
        String what = spec.field() == null ? "books" : fieldWord(spec.field());
        return topGroup + " has the most " + what + " (" + formatValue(spec.field(), topValue) + ") among "
            + pluralize(t.rows().size(), groupWord(spec.groupBy()).toLowerCase(Locale.ROOT)) + "."
            + (desc.isEmpty() ? "" : " (" + desc + ")");
    }

    public String listSummary(QuerySpec spec, boolean admin, boolean albanian, Table t) {
        if (t.rows().isEmpty()) return noMatchMessage(spec, admin, albanian);
        String desc = naturalFilters(spec.filters(), albanian);
        if (spec.orderBy() != null) {
            String title = String.valueOf(t.rows().get(0).get(1));
            if (albanian) {
                String verb = spec.orderBy().direction() == QuerySpec.Direction.ASC ? "më të ulët" : "më të lartë";
                String who = admin ? "në bibliotekë" : "që ke";
                return "Libri me " + fieldWordAl(spec.orderBy().field()) + " " + verb + " " + who
                    + " është '" + title + "'." + (desc.isEmpty() ? "" : " (" + desc + ")");
            }
            String verb = spec.orderBy().direction() == QuerySpec.Direction.ASC ? "lowest" : "highest";
            String where = admin ? "in the whole library" : "in your library";
            return "The book with the " + verb + " " + fieldWord(spec.orderBy().field()) + " " + where
                + " is '" + title + "'." + (desc.isEmpty() ? "" : " (" + desc + ")");
        }
        if (albanian) {
            String who = admin ? "në bibliotekë" : "që ke";
            return pluralizeAl(t.rows().size(), "libër", "libra") + " " + who + (desc.isEmpty() ? "" : " " + desc)
                + " janë më poshtë.";
        }
        String where = admin ? "in the whole library" : "in your library";
        return pluralize(t.rows().size(), "book") + " " + where + (desc.isEmpty() ? "" : " " + desc)
            + " are shown below.";
    }

    /** Friendly label for a status, e.g. "completed" / "duke i lexuar" - never the raw enum name. */
    static String statusLabel(BookStatus status, boolean albanian) {
        if (albanian) {
            return switch (status) {
                case TO_READ -> "për t'u lexuar";
                case READING -> "duke i lexuar";
                case COMPLETED -> "të përfunduara";
            };
        }
        return switch (status) {
            case TO_READ -> "to read";
            case READING -> "currently reading";
            case COMPLETED -> "completed";
        };
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

    static String pluralize(long count, String noun) {
        return INTEGER_FORMAT.format(count) + " " + noun + (count == 1 ? "" : "s");
    }

    /** Albanian singular/plural noun pair, e.g. (1, "libër", "libra") -> "1 libër". */
    static String pluralizeAl(long count, String singular, String plural) {
        return INTEGER_FORMAT.format(count) + " " + (count == 1 ? singular : plural);
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

    private static String fieldWordAl(Field field) {
        if (field == null) return "libra";
        return switch (field) {
            case PRICE -> "çmim";
            case PAGES -> "faqe";
            case TITLE -> "titull";
            case AUTHOR -> "autor";
            case GENRE -> "zhanër";
            case STATUS -> "status";
            case CREATED_AT -> "datën e shtimit";
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

    private static String groupWordAl(GroupBy groupBy) {
        return switch (groupBy) {
            case GENRE -> "zhanër";
            case AUTHOR -> "autor";
            case STATUS -> "status";
            case USER -> "përdorues";
        };
    }

    /** Capitalizes just the first letter, e.g. "fantasy" -> "Fantasy". */
    static String capitalize(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    /** Capitalizes every word, e.g. "frank herbert" -> "Frank Herbert". */
    static String titleCase(String s) {
        String[] words = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(capitalize(w));
        }
        return sb.toString();
    }
}
