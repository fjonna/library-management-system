package com.fjonna.library.agent;

import com.fjonna.library.book.BookStatus;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * English + Albanian keyword catalogs, and fuzzy "does this question contain X" helpers built on
 * {@link EditDistance} so a small typo ("expesnive", "fantsy") still matches. Stateless: safe to
 * share a single instance.
 */
public class Vocabulary {

    // ---- aggregate cues ---------------------------------------------------------------------
    static final List<String> COUNT_WORDS = List.of("how many", "count", "number of", "numri", "sa");
    static final List<String> SUM_WORDS = List.of(
        "total", "sum", "altogether", "together", "in total", "spent", "worth", "how much money",
        "bashke", "gjithsej", "gjithe", "vlera totale", "sa kam shpenzuar", "sa vlejne", "sa para");
    static final List<String> AVG_WORDS = List.of("average", "avg", "mean", "mesatar", "mesatarisht", "mesatarja");

    record MinMaxRule(List<String> phrases, Aggregate aggregate, Field defaultField) {
    }

    /** Checked in order: specific phrases (which imply both direction and field) before generic ones. */
    static final List<MinMaxRule> MIN_MAX_RULES = List.of(
        new MinMaxRule(List.of(
            "cheapest", "e lira", "i lire", "lire", "me i liri", "me e lira", "lowest price",
            "cmimi me i ulet", "cmim te ulet"), Aggregate.MIN, Field.PRICE),
        new MinMaxRule(List.of("shortest", "me i shkurtri", "me e shkurtra"), Aggregate.MIN, Field.PAGES),
        new MinMaxRule(List.of(
            "most expensive", "expensive", "priciest", "costly", "shtrenjte", "e shtrenjta", "i shtrenjti",
            "highest price", "cmimi me i larte", "cmim te larte"), Aggregate.MAX, Field.PRICE),
        new MinMaxRule(List.of(
            "longest", "most pages", "thickest", "me i gjati", "me e gjate", "me shume faqe", "faqe me shume"),
            Aggregate.MAX, Field.PAGES),
        new MinMaxRule(List.of("lowest", "least", "minimum", "me pak", "me i liri"), Aggregate.MIN, Field.PRICE),
        new MinMaxRule(List.of("highest", "maximum", "me shume"), Aggregate.MAX, Field.PRICE)
    );

    static final List<String> RECENT_WORDS = List.of(
        "recent", "latest", "newest", "librat e fundit", "me te fundit", "se fundmi", "libra te rinj");

    // ---- field cues ---------------------------------------------------------------------------
    static final List<String> PAGES_WORDS = List.of("pages", "page", "faqe", "faqet");
    static final List<String> PRICE_WORDS = List.of(
        "price", "cost", "value", "worth", "dollars", "dollar", "euro", "cmim", "kosto", "vlera", "shpenzuar");
    static final List<String> TITLE_WORDS = List.of("title", "titled", "titull", "titullin");
    static final List<String> AUTHOR_WORDS = List.of("author", "autor", "shkrimtar");
    static final List<String> GENRE_WORDS = List.of("genre", "zhanr", "gjini");
    static final List<String> STATUS_WORDS = List.of("status");

    // ---- grouping / ranking cues ----------------------------------------------------------------
    static final List<String> GROUP_BY_GENRE_WORDS = List.of(
        "per genre", "by genre", "each genre", "genre breakdown", "genres",
        "sipas gjinise", "per gjini", "gjinite", "zhanret", "zhanre");
    static final List<String> GROUP_BY_AUTHOR_WORDS = List.of(
        "per author", "by author", "each author", "per autor", "sipas autorit", "autoret");
    static final List<String> SUPERLATIVE_WORDS = List.of("most", "more", "top", "shume");
    static final List<String> OWNER_RANKING_WORDS = List.of(
        "who owns", "who has", "which user", "which person", "which member", "each user", "per user",
        "kush ka", "cili perdorues", "cili anetar", "secili perdorues");
    static final List<String> POPULAR_WORDS = List.of(
        "popular", "common", "bestsell", "popullor", "popullarizuar", "me i njohur", "me i shitur", "me i lexuar");

    // ---- status cues ----------------------------------------------------------------------------
    static final List<String> TO_READ_WORDS = List.of(
        "to read", "to-read", "unread", "not read", "want to read", "wishlist", "backlog",
        "per te lexuar", "dua te lexoj", "pa lexuar", "per lexim", "per tu lexuar",
        "nuk i kam lexuar", "nuk e kam lexuar", "nuk kam lexuar", "nuk jane lexuar");
    static final List<String> READING_WORDS = List.of("reading", "in progress", "duke lexuar", "ne lexim");
    static final List<String> COMPLETED_WORDS = List.of(
        "completed", "finished", "already read", "have read", "read already",
        "lexuar", "perfunduar", "kam lexuar", "i lexuar");

    /** Generic "show me books" catch-all: present -> LIST_BOOKS, absent (and nothing else matched) -> UNKNOWN. */
    static final List<String> GENERIC_BOOK_WORDS = List.of(
        "book", "list", "libra", "liber", "libri", "tregoni", "trego", "shfaq");

    // ---- search markers ---------------------------------------------------------------------------
    static final List<String> SEARCH_MARKERS = List.of(
        "titled", "called", "named", "search for", "find the book", "find book",
        "me titullin", "titullin", "titulli", "quhet", "kerko per", "kerko", "gjej librin");

    // ---- numbers --------------------------------------------------------------------------------
    static final Map<String, Integer> NUMBER_WORDS = Map.ofEntries(
        Map.entry("one", 1), Map.entry("two", 2), Map.entry("three", 3), Map.entry("four", 4),
        Map.entry("five", 5), Map.entry("six", 6), Map.entry("seven", 7), Map.entry("eight", 8),
        Map.entry("nine", 9), Map.entry("ten", 10), Map.entry("eleven", 11), Map.entry("twelve", 12),
        Map.entry("thirteen", 13), Map.entry("fourteen", 14), Map.entry("fifteen", 15),
        Map.entry("sixteen", 16), Map.entry("seventeen", 17), Map.entry("eighteen", 18),
        Map.entry("nineteen", 19), Map.entry("twenty", 20),
        Map.entry("nje", 1), Map.entry("dy", 2), Map.entry("tre", 3), Map.entry("kater", 4),
        Map.entry("pese", 5), Map.entry("gjashte", 6), Map.entry("shtate", 7), Map.entry("tete", 8),
        Map.entry("nente", 9), Map.entry("dhjete", 10));

    private static final Pattern DIGITS = Pattern.compile("\\b(\\d{1,2})\\b");
    private static final Pattern NUMBER_WORD = Pattern.compile("\\b(" + String.join("|", NUMBER_WORDS.keySet()) + ")\\b");

    /**
     * Exact-token markers used only to pick which language to answer in. Deliberately excludes
     * short words that collide with real English usage in this app (e.g. "per" in "per genre",
     * "a"/"e"/"ne" as English letters/words) - a false positive here would answer an English
     * question in Albanian, which is worse than an occasional missed Albanian question.
     */
    private static final List<String> ALBANIAN_MARKERS = List.of(
        "sa", "libra", "liber", "librat", "libri", "librin", "librit", "kam", "kush", "cili", "cila",
        "cilat", "eshte", "jane", "nuk", "nga", "autor", "gjini", "zhanr", "zhanret", "shtrenjte",
        "lire", "mesatar", "mesatarisht", "bashke", "gjithe", "perfunduar", "lexuar", "lexoj", "faqe",
        "cmim", "kosto", "vlera", "te", "dhe", "kjo", "une", "ka", "qe", "pa", "mbi", "nen", "kerko",
        "gjej", "secili", "cdo", "keni", "ke", "duke", "shfaq", "trego", "tregoni");

    /** True if the question looks Albanian (see {@link #ALBANIAN_MARKERS}); otherwise treated as English. */
    public static boolean isAlbanian(String question) {
        for (String word : Normalizer.words(Normalizer.normalize(question))) {
            if (ALBANIAN_MARKERS.contains(word)) return true;
        }
        return false;
    }

    /** True if any of the phrases appears in {@code q}, allowing a one/two-character typo per word. */
    public boolean any(String q, Collection<String> phrases) {
        for (String phrase : phrases) {
            if (fuzzyContains(q, phrase)) return true;
        }
        return false;
    }

    /** Fuzzy, word-by-word phrase match: every word of {@code phrase} must appear, in order, each within tolerance. */
    public boolean fuzzyContains(String q, String phrase) {
        String[] qWords = q.trim().isEmpty() ? new String[0] : q.trim().split("\\s+");
        String[] pWords = phrase.split("\\s+");
        outer:
        for (int i = 0; i + pWords.length <= qWords.length; i++) {
            for (int j = 0; j < pWords.length; j++) {
                String typed = Normalizer.stripPunctuation(qWords[i + j]);
                if (!EditDistance.closeEnough(typed, pWords[j])) continue outer;
            }
            return true;
        }
        return false;
    }

    public BookStatus matchStatus(String q) {
        if (any(q, TO_READ_WORDS)) return BookStatus.TO_READ;
        if (any(q, READING_WORDS)) return BookStatus.READING;
        if (any(q, COMPLETED_WORDS)) return BookStatus.COMPLETED;
        return null;
    }

    public Field matchExplicitField(String q) {
        if (any(q, PAGES_WORDS)) return Field.PAGES;
        if (any(q, PRICE_WORDS)) return Field.PRICE;
        if (any(q, AUTHOR_WORDS)) return Field.AUTHOR;
        if (any(q, GENRE_WORDS)) return Field.GENRE;
        if (any(q, TITLE_WORDS)) return Field.TITLE;
        if (any(q, STATUS_WORDS)) return Field.STATUS;
        return null;
    }

    public Integer matchNumber(String q) {
        var d = DIGITS.matcher(q);
        if (d.find()) return Integer.parseInt(d.group(1));
        var w = NUMBER_WORD.matcher(q);
        if (w.find()) return NUMBER_WORDS.get(w.group(1));
        return null;
    }

    /**
     * Returns the longest known value (genre/author, lowercase) that appears in {@code q}, exactly or
     * with a per-word typo tolerance ("fantsy" ~ "fantasy"). User-supplied text, but it is only ever
     * used as a bound parameter value downstream - never concatenated into SQL. Falls back to
     * matching just the last word of a multi-word value (e.g. "Orwell" for "George Orwell") so a
     * question naming only a surname still resolves to the full stored name.
     */
    public String findKnownValue(String q, Collection<String> known) {
        String full = bestMatch(q, known, false);
        return full != null ? full : bestMatch(q, known, true);
    }

    private String bestMatch(String q, Collection<String> known, boolean lastWordOnly) {
        String best = null;
        for (String k : known) {
            if (k == null || k.isBlank()) continue;
            String kLower = k.toLowerCase(Locale.ROOT);
            String kNorm = Normalizer.normalize(kLower);
            String candidate = kNorm;
            if (lastWordOnly) {
                String[] parts = kNorm.split("\\s+");
                if (parts.length < 2 || parts[parts.length - 1].length() < 4) continue;
                candidate = parts[parts.length - 1];
            }
            if (fuzzyContains(q, candidate) && (best == null || kLower.length() > best.length())) {
                best = kLower;
            }
        }
        return best;
    }
}
