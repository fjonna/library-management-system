package com.fjonna.library.agent;

import com.fjonna.library.agent.QuerySpec.Direction;
import com.fjonna.library.agent.QuerySpec.Filters;
import com.fjonna.library.agent.QuerySpec.OrderBy;
import com.fjonna.library.book.BookStatus;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rule-based English/Albanian parser that turns a free-text question into a {@link QuerySpec}:
 * what to compute, over which rows, grouped how, ordered how, how many. It never produces SQL -
 * that is {@link SqlBuilder}'s job. Keyword and known-value (genre/author) matching tolerates a
 * small typo via {@link EditDistance} (through {@link Vocabulary}).
 */
public class SpecParser {

    private final Vocabulary vocab = new Vocabulary();

    private static final Pattern BETWEEN_EN = Pattern.compile(
        "\\bbetween\\s*[$€]?\\s*(\\d+(?:[.,]\\d{1,2})?)\\s*(?:and|-)\\s*[$€]?\\s*(\\d+(?:[.,]\\d{1,2})?)"
            + "\\s*(pages|page|faqe|faqet|dollars?|euro)?");
    private static final Pattern BETWEEN_AL = Pattern.compile(
        "\\bmes\\s*[$€]?\\s*(\\d+(?:[.,]\\d{1,2})?)\\s*dhe\\s*[$€]?\\s*(\\d+(?:[.,]\\d{1,2})?)"
            + "\\s*(pages|page|faqe|faqet|dollars?|euro)?");
    private static final Pattern MAX_THRESHOLD = Pattern.compile(
        "\\b(?:under|below|less than|cheaper than|up to|within|nen|me pak se|deri ne)\\b\\s*[$€]?\\s*"
            + "(\\d+(?:[.,]\\d{1,2})?)\\s*(pages|page|faqe|faqet|dollars?|euro)?");
    private static final Pattern MIN_THRESHOLD = Pattern.compile(
        "\\b(?:over|above|more than|mbi|me shume se)\\b\\s*[$€]?\\s*"
            + "(\\d+(?:[.,]\\d{1,2})?)\\s*(pages|page|faqe|faqet|dollars?|euro)?");

    private record MinMaxMatch(Aggregate aggregate, Field field) {
    }

    private static final class Range {
        BigDecimal min;
        BigDecimal max;

        boolean hasAny() {
            return min != null || max != null;
        }
    }

    public QuerySpec parse(String question, Collection<String> genres, Collection<String> authors) {
        String q = Normalizer.normalize(question).trim();

        BookStatus status = vocab.matchStatus(q);

        Range price = new Range();
        Range pages = new Range();
        q = consumeBetween(q, price, pages);
        q = consumeThreshold(q, MIN_THRESHOLD, true, price, pages);
        q = consumeThreshold(q, MAX_THRESHOLD, false, price, pages);

        String genre = vocab.findKnownValue(q, genres);
        String author = vocab.findKnownValue(q, authors);
        String titleContains = searchText(q);
        Integer limit = vocab.matchNumber(q);

        GroupBy groupBy = groupBy(q);
        Aggregate aggregate;
        Field field;
        OrderBy orderBy = null;

        if (groupBy != null) {
            Field groupField = fieldForGrouping(q, groupBy);
            aggregate = groupField == null ? Aggregate.GROUP_COUNT : pickAggregateForField(q);
            field = groupField;
        } else if (titleContains != null) {
            aggregate = Aggregate.LIST;
            field = null;
        } else if (vocab.any(q, Vocabulary.AVG_WORDS)) {
            aggregate = Aggregate.AVG;
            field = explicitFieldOr(q, Field.PRICE);
        } else if (vocab.any(q, Vocabulary.SUM_WORDS) && explicitPriceOrPagesField(q) != null) {
            aggregate = Aggregate.SUM;
            field = explicitPriceOrPagesField(q);
        } else {
            MinMaxMatch mm = matchMinMax(q);
            if (mm != null) {
                aggregate = Aggregate.LIST;
                field = null;
                orderBy = new OrderBy(mm.field(), mm.aggregate() == Aggregate.MIN ? Direction.ASC : Direction.DESC);
            } else if (vocab.any(q, Vocabulary.RECENT_WORDS)) {
                aggregate = Aggregate.LIST;
                field = null;
                orderBy = new OrderBy(Field.CREATED_AT, Direction.DESC);
            } else if (vocab.any(q, Vocabulary.SUM_WORDS)) {
                aggregate = Aggregate.SUM;
                field = Field.PRICE;
            } else if (price.hasAny() || pages.hasAny()) {
                aggregate = Aggregate.LIST;
                field = null;
            } else if (vocab.any(q, Vocabulary.COUNT_WORDS)) {
                aggregate = Aggregate.COUNT;
                field = null;
            } else {
                aggregate = Aggregate.LIST;
                field = null;
            }
        }

        Filters filters = new Filters(genre, author, status, titleContains,
            price.min, price.max,
            pages.min == null ? null : pages.min.intValue(),
            pages.max == null ? null : pages.max.intValue());

        return new QuerySpec(aggregate, field, filters, groupBy, orderBy, limit);
    }

    /** True if the fully-parsed spec carries no recognizable signal at all (used to trigger "did you mean"). */
    public boolean isEmpty(QuerySpec spec) {
        return spec.aggregate() == Aggregate.LIST && spec.field() == null && spec.groupBy() == null
            && spec.orderBy() == null && spec.limit() == null && spec.filters().isEmpty();
    }

    /** Also used by {@link QueryParser} to recognize "which is the most popular book" independent of the spec. */
    boolean isPopularQuestion(String question) {
        return vocab.any(Normalizer.normalize(question), Vocabulary.POPULAR_WORDS);
    }

    boolean hasGenericBookCue(String question) {
        return vocab.any(Normalizer.normalize(question), Vocabulary.GENERIC_BOOK_WORDS);
    }

    boolean isRecentPhrase(String question) {
        return vocab.any(Normalizer.normalize(question), Vocabulary.RECENT_WORDS);
    }

    boolean isOwnerRankingPhrase(String question) {
        return vocab.any(Normalizer.normalize(question), Vocabulary.OWNER_RANKING_WORDS);
    }

    private GroupBy groupBy(String q) {
        if (vocab.any(q, Vocabulary.OWNER_RANKING_WORDS)) return GroupBy.USER;
        if (vocab.any(q, Vocabulary.GROUP_BY_GENRE_WORDS)) return GroupBy.GENRE;
        if (vocab.any(q, Vocabulary.GROUP_BY_AUTHOR_WORDS)) return GroupBy.AUTHOR;
        if (vocab.any(q, Vocabulary.AUTHOR_WORDS) && vocab.any(q, Vocabulary.SUPERLATIVE_WORDS)) return GroupBy.AUTHOR;
        return null;
    }

    /** Only USER grouping can target a scalar field (total/average pages or price per user). */
    private Field fieldForGrouping(String q, GroupBy groupBy) {
        if (groupBy != GroupBy.USER) return null;
        return explicitPriceOrPagesField(q);
    }

    private Aggregate pickAggregateForField(String q) {
        if (vocab.any(q, Vocabulary.AVG_WORDS)) return Aggregate.AVG;
        return Aggregate.SUM;
    }

    private MinMaxMatch matchMinMax(String q) {
        for (Vocabulary.MinMaxRule rule : Vocabulary.MIN_MAX_RULES) {
            if (vocab.any(q, rule.phrases())) {
                Field explicit = explicitPriceOrPagesField(q);
                return new MinMaxMatch(rule.aggregate(), explicit != null ? explicit : rule.defaultField());
            }
        }
        return null;
    }

    private Field explicitPriceOrPagesField(String q) {
        Field f = vocab.matchExplicitField(q);
        return (f == Field.PRICE || f == Field.PAGES) ? f : null;
    }

    private Field explicitFieldOr(String q, Field fallback) {
        Field f = explicitPriceOrPagesField(q);
        return f != null ? f : fallback;
    }

    private String consumeBetween(String q, Range price, Range pages) {
        Matcher m = BETWEEN_EN.matcher(q);
        boolean found = m.find();
        if (!found) {
            m = BETWEEN_AL.matcher(q);
            found = m.find();
        }
        if (!found) return q;
        applyRange(m.group(1), m.group(2), m.group(3), price, pages);
        return cut(q, m.start(), m.end());
    }

    private String consumeThreshold(String q, Pattern pattern, boolean isMin, Range price, Range pages) {
        Matcher m = pattern.matcher(q);
        if (!m.find()) return q;
        String unit = m.group(2);
        if (isPagesUnit(unit)) {
            BigDecimal v = parseDecimal(m.group(1));
            if (isMin) pages.min = v; else pages.max = v;
        } else {
            BigDecimal v = parseDecimal(m.group(1));
            if (isMin) price.min = v; else price.max = v;
        }
        return cut(q, m.start(), m.end());
    }

    private void applyRange(String g1, String g2, String unit, Range price, Range pages) {
        Range target = isPagesUnit(unit) ? pages : price;
        target.min = parseDecimal(g1);
        target.max = parseDecimal(g2);
    }

    private static boolean isPagesUnit(String unit) {
        if (unit == null) return false;
        String u = unit.toLowerCase(Locale.ROOT);
        return u.startsWith("page") || u.startsWith("faqe");
    }

    private static BigDecimal parseDecimal(String raw) {
        return new BigDecimal(raw.replace(',', '.'));
    }

    private static String cut(String q, int start, int end) {
        return (q.substring(0, start) + " " + q.substring(end)).trim();
    }

    /** Returns the free text following a search marker (e.g. "titled", "me titullin"), or null if none found. */
    private String searchText(String q) {
        int bestEnd = -1;
        for (String marker : Vocabulary.SEARCH_MARKERS) {
            int idx = q.indexOf(marker);
            if (idx >= 0) {
                int end = idx + marker.length();
                if (end > bestEnd) bestEnd = end;
            }
        }
        if (bestEnd < 0) return null;
        String rest = q.substring(bestEnd).trim();
        rest = rest.replaceAll("^[:\\-]+", "").trim();
        rest = rest.replaceAll("^['\"]+", "").replaceAll("[\"'?.!]+$", "").trim();
        return rest.isEmpty() ? null : rest;
    }
}
