package com.fjonna.library.agent;

import com.fjonna.library.book.BookStatus;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Rule-based parser: turns a question into an {@link Intent} plus optional filters.
 * It never produces SQL; every intent maps to a fixed, parameterized query.
 * Understands English and Albanian keywords. Albanian input is matched with
 * diacritics stripped (ë→e, ç→c) so "shtrenjtë" and "shtrenjte" both match.
 */
@Component
public class QueryParser {

    public record Parsed(Intent intent, Integer limit, String genre, String author, BookStatus status,
                          BigDecimal price, String searchText) {
    }

    private static final Map<String, Integer> WORDS = Map.ofEntries(
        Map.entry("two", 2), Map.entry("three", 3), Map.entry("four", 4), Map.entry("five", 5),
        Map.entry("six", 6), Map.entry("seven", 7), Map.entry("eight", 8), Map.entry("nine", 9), Map.entry("ten", 10),
        Map.entry("dy", 2), Map.entry("tre", 3), Map.entry("kater", 4), Map.entry("pese", 5),
        Map.entry("gjashte", 6), Map.entry("shtate", 7), Map.entry("tete", 8), Map.entry("nente", 9),
        Map.entry("dhjete", 10));
    private static final Pattern DIGITS = Pattern.compile("\\b(\\d{1,2})\\b");
    private static final Pattern WORD = Pattern.compile(
        "\\b(two|three|four|five|six|seven|eight|nine|ten|dy|tre|kater|pese|gjashte|shtate|tete|nente|dhjete)\\b");
    private static final Pattern WHO = Pattern.compile(
        "\\b(who|which (user|person|member)|kush|cili (perdorues|anetar))\\b");
    private static final Pattern PRICE_THRESHOLD = Pattern.compile(
        "\\b(?:under|below|less than|cheaper than|up to|within|nen|me pak se|deri ne)\\b\\s*[$€]?\\s*(\\d+(?:[.,]\\d{1,2})?)");
    private static final List<String> SEARCH_MARKERS = List.of(
        "titled", "called", "named", "search for", "find the book", "find book",
        "me titullin", "titullin", "titulli", "quhet", "kerko per", "kerko", "gjej librin");

    public Parsed parse(String question, Collection<String> genres, Collection<String> authors) {
        String q = normalize(question).trim();
        String genre = findMention(q, genres);
        String author = findMention(q, authors);
        BookStatus status = status(q);

        Matcher priceMatcher = PRICE_THRESHOLD.matcher(q);
        BigDecimal price = null;
        String qForLimit = q;
        if (priceMatcher.find()) {
            price = parsePrice(priceMatcher.group(1));
            qForLimit = q.substring(0, priceMatcher.start()) + " " + q.substring(priceMatcher.end());
        }
        Integer limit = limit(qForLimit);
        String searchText = searchText(q);

        Intent intent = intent(q, genre, author, status, price != null, searchText != null);
        return new Parsed(intent, limit, genre, author, status, price, searchText);
    }

    private Intent intent(String q, String genre, String author, BookStatus status,
                           boolean hasPriceThreshold, boolean hasSearchText) {
        if (WHO.matcher(q).find() && has(q, "most", "more", "shume")
            && has(q, "book", "libra", "liber")
            && !has(q, "expensive", "cheap", "pages", "long", "popular",
                     "shtrenjte", "lire", "faqe", "gjate", "popullor", "autor")) {
            return Intent.MOST_BOOKS_OWNER;
        }
        if (has(q, "author", "autor") && has(q, "most", "more", "top", "shume")) {
            return Intent.TOP_AUTHOR;
        }
        if (has(q, "popular", "common", "bestsell", "popullor", "me i njohur", "me i shitur", "me i lexuar")) {
            return Intent.MOST_POPULAR_BOOK;
        }
        if (has(q, "per genre", "by genre", "each genre", "genre breakdown", "genres",
                 "sipas gjinise", "per gjini", "gjinite", "zhanret", "zhanre")) {
            return Intent.GENRE_BREAKDOWN;
        }
        if (hasPriceThreshold) {
            return Intent.BOOKS_UNDER_PRICE;
        }
        if (has(q, "average price", "avg price", "average cost", "cmimi mesatar", "mesatarja e cmimit", "cmim mesatar")) {
            return Intent.AVERAGE_PRICE;
        }
        if (has(q, "cheap", "lowest price", "least expensive", "lire", "e lira", "i lire", "cmim te ulet", "cmimi me i ulet")) {
            return Intent.CHEAPEST;
        }
        if (has(q, "expensive", "priciest", "costly", "highest price",
                 "shtrenjte", "e shtrenjta", "i shtrenjti", "cmim te larte", "cmimi me i larte")) {
            return Intent.TOP_EXPENSIVE;
        }
        if (has(q, "longest", "most pages", "thickest", "me i gjate", "me e gjate", "me shume faqe", "faqe me shume")) {
            return Intent.LONGEST;
        }
        if (has(q, "recent", "latest", "newest", "librat e fundit", "me te fundit", "se fundmi", "libra te rinj")) {
            return Intent.RECENT_BOOKS;
        }
        if (has(q, "spent", "total value", "total cost", "total price", "worth", "how much money",
                 "vlera totale", "sa kam shpenzuar", "sa vlejne", "sa para", "vlera e librave")) {
            return Intent.TOTAL_VALUE;
        }
        if (has(q, "how many", "count", "number of", "numri i librave")
            || (q.contains("sa") && has(q, "libra", "liber", "libri"))) {
            return Intent.COUNT_BOOKS;
        }
        if (hasSearchText) {
            return Intent.SEARCH_TITLE;
        }
        if (genre != null || author != null || status != null
            || has(q, "book", "list", "libra", "liber", "libri", "tregoni", "trego", "shfaq")) {
            return Intent.LIST_BOOKS;
        }
        return Intent.UNKNOWN;
    }

    private BookStatus status(String q) {
        if (has(q, "to read", "to-read", "unread", "not read", "want to read", "wishlist", "backlog",
                 "per te lexuar", "dua te lexoj", "pa lexuar", "per lexim")) {
            return BookStatus.TO_READ;
        }
        if (has(q, "reading", "in progress", "duke lexuar", "ne lexim")) {
            return BookStatus.READING;
        }
        if (has(q, "completed", "finished", "already read", "have read", "read already",
                 "lexuar", "perfunduar", "kam lexuar", "i lexuar")) {
            return BookStatus.COMPLETED;
        }
        return null;
    }

    private Integer limit(String q) {
        Matcher d = DIGITS.matcher(q);
        if (d.find()) return Integer.parseInt(d.group(1));
        Matcher w = WORD.matcher(q);
        if (w.find()) return WORDS.get(w.group(1));
        return null;
    }

    private static BigDecimal parsePrice(String raw) {
        return new BigDecimal(raw.replace(',', '.'));
    }

    /** Returns the free text following a search marker (e.g. "titled", "me titullin"), or null if none found. */
    private String searchText(String q) {
        int bestEnd = -1;
        for (String marker : SEARCH_MARKERS) {
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

    /** Returns the longest known value (genre/author, lowercase) that appears as a whole word in the question. */
    private String findMention(String q, Collection<String> known) {
        String best = null;
        for (String k : known) {
            if (k == null || k.isBlank()) continue;
            String kLower = k.toLowerCase(Locale.ROOT);
            String kNorm = normalize(kLower);
            Pattern p = Pattern.compile("(?<![\\p{L}\\d])" + Pattern.quote(kNorm) + "(?![\\p{L}\\d])");
            if (p.matcher(q).find() && (best == null || kLower.length() > best.length())) {
                best = kLower;
            }
        }
        return best;
    }

    private static String normalize(String s) {
        return s.toLowerCase(Locale.ROOT).replace('ë', 'e').replace('ç', 'c');
    }

    private static boolean has(String q, String... needles) {
        for (String n : needles) {
            if (q.contains(n)) return true;
        }
        return false;
    }
}
