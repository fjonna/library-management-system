package com.fjonna.library.agent;

import com.fjonna.library.book.BookStatus;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Rule-based parser: turns a question into an {@link Intent} plus optional filters.
 * It never produces SQL; every intent maps to a fixed, parameterized query.
 */
@Component
public class QueryParser {

    public record Parsed(Intent intent, Integer limit, String genre, String author, BookStatus status) {
    }

    private static final Map<String, Integer> WORDS = Map.of(
        "two", 2, "three", 3, "four", 4, "five", 5, "six", 6,
        "seven", 7, "eight", 8, "nine", 9, "ten", 10);
    private static final Pattern DIGITS = Pattern.compile("\\b(\\d{1,2})\\b");
    private static final Pattern WORD = Pattern.compile("\\b(two|three|four|five|six|seven|eight|nine|ten)\\b");
    private static final Pattern WHO = Pattern.compile("\\b(who|which (user|person|member))\\b");

    public Parsed parse(String question, Collection<String> genres, Collection<String> authors) {
        String q = question.toLowerCase(Locale.ROOT).trim();
        String genre = findMention(q, genres);
        String author = findMention(q, authors);
        BookStatus status = status(q);
        Integer limit = limit(q);
        return new Parsed(intent(q, genre, author, status), limit, genre, author, status);
    }

    private Intent intent(String q, String genre, String author, BookStatus status) {
        if (WHO.matcher(q).find() && q.contains("most") && q.contains("book")
            && !has(q, "expensive", "cheap", "pages", "long", "popular")) {
            return Intent.MOST_BOOKS_OWNER;
        }
        if (has(q, "popular", "common", "bestsell")) return Intent.MOST_POPULAR_BOOK;
        if (has(q, "per genre", "by genre", "each genre", "genre breakdown", "genres")) return Intent.GENRE_BREAKDOWN;
        if (has(q, "cheap", "lowest price", "least expensive")) return Intent.CHEAPEST;
        if (has(q, "expensive", "priciest", "costly", "highest price")) return Intent.TOP_EXPENSIVE;
        if (has(q, "longest", "most pages", "thickest")) return Intent.LONGEST;
        if (has(q, "spent", "total value", "total cost", "total price", "worth", "how much money")) return Intent.TOTAL_VALUE;
        if (has(q, "how many", "count", "number of")) return Intent.COUNT_BOOKS;
        if (genre != null || author != null || status != null || has(q, "book", "list")) return Intent.LIST_BOOKS;
        return Intent.UNKNOWN;
    }

    private BookStatus status(String q) {
        if (has(q, "completed", "finished", "already read", "have read", "read already")) return BookStatus.COMPLETED;
        if (has(q, "to read", "to-read", "unread", "not read", "want to read", "wishlist", "backlog")) return BookStatus.TO_READ;
        if (has(q, "reading", "in progress")) return BookStatus.READING;
        return null;
    }

    private Integer limit(String q) {
        Matcher d = DIGITS.matcher(q);
        if (d.find()) return Integer.parseInt(d.group(1));
        Matcher w = WORD.matcher(q);
        if (w.find()) return WORDS.get(w.group(1));
        return null;
    }

    /** Returns the longest known value (genre/author, lowercase) that appears as a whole word in the question. */
    private String findMention(String q, Collection<String> known) {
        String best = null;
        for (String k : known) {
            if (k == null || k.isBlank()) continue;
            Pattern p = Pattern.compile("(?<![\\p{L}\\d])" + Pattern.quote(k.toLowerCase(Locale.ROOT)) + "(?![\\p{L}\\d])");
            if (p.matcher(q).find() && (best == null || k.length() > best.length())) {
                best = k.toLowerCase(Locale.ROOT);
            }
        }
        return best;
    }

    private static boolean has(String q, String... needles) {
        for (String n : needles) {
            if (q.contains(n)) return true;
        }
        return false;
    }
}
