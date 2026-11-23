package com.fjonna.library.agent;

import com.fjonna.library.agent.AgentRepository.Filter;
import com.fjonna.library.agent.AgentRepository.Sort;
import com.fjonna.library.agent.AgentRepository.Table;
import com.fjonna.library.agent.QueryParser.Parsed;
import com.fjonna.library.book.BookStatus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AgentService {

    private static final int MAX_ROWS = 50;
    private static final int MAX_EXAMPLES_USER = 6;
    private static final int MAX_EXAMPLES_ADMIN = 8;
    private static final Random RANDOM = new Random();

    /** Pure grammar/function words excluded from "did you mean" scoring so they don't inflate overlap. */
    private static final Set<String> STOPWORDS = Set.of(
        "what", "is", "the", "a", "an", "of", "my", "i", "do", "does", "did", "are", "am", "to",
        "in", "on", "for", "and", "it", "this", "that", "have", "has", "me", "you",
        "cili", "cila", "eshte", "te", "e", "dhe", "per", "ne", "nga", "kam", "ka", "kjo", "une");

    /** Safe for any user to ask: never ranks or lists other users. */
    public static final List<String> COMMON_EXAMPLES = List.of(
        "Show the five most expensive books",
        "Show my cheapest 3 books",
        "How many books have I completed?",
        "Show books I am currently reading",
        "How many books per genre?",
        "What is the total value of my books?",
        "Show my longest books",
        "Show books under $20",
        "What is the average price of my books?",
        "Which author has the most books?",
        "Show my most recent books",
        "Find the book titled Dune",
        "Cilat janë librat më të shtrenjtë?",
        "Sa libra kam?",
        "Librat më të fundit",
        "Kërko librin me titullin Dune");

    /** Only shown to admins: these rank or list information about other users. */
    public static final List<String> ADMIN_ONLY_EXAMPLES = List.of(
        "Who owns the most books?",
        "Which is the most popular book?",
        "Kush ka më shumë libra?");

    private final QueryParser parser;
    private final AgentRepository repo;

    /** Pure helpers for the generic (slot-based) engine used once the legacy intent lookup says UNKNOWN. */
    private final SpecParser specHelper = new SpecParser();
    private final SqlBuilder sqlBuilder = new SqlBuilder();
    private final SummaryFormatter summaryFormatter = new SummaryFormatter();

    /** Role-aware, shuffled subset for the "examples" endpoint and chip suggestions. */
    public List<String> examples(boolean admin) {
        return pickExamples(admin, admin ? MAX_EXAMPLES_ADMIN : MAX_EXAMPLES_USER);
    }

    private List<String> pickExamples(boolean admin, int max) {
        List<String> pool = new ArrayList<>(COMMON_EXAMPLES);
        if (admin) pool.addAll(ADMIN_ONLY_EXAMPLES);
        Collections.shuffle(pool, RANDOM);
        return pool.subList(0, Math.min(max, pool.size()));
    }

    @Transactional(readOnly = true)
    public QueryResult answer(UUID userId, boolean admin, String question) {
        UUID scope = admin ? null : userId;
        List<String> genres = repo.genres(scope);
        List<String> authors = repo.authors(scope);
        Parsed p = parser.parse(question, genres, authors);
        Filter f = new Filter(scope, p.genre(), p.author(), p.status());
        int limit = p.limit() == null ? 0 : Math.min(Math.max(p.limit(), 1), MAX_ROWS);
        int top = limit == 0 ? 5 : limit;
        boolean albanian = Vocabulary.isAlbanian(question);
        String understood = understoodAs(p, admin);

        return switch (p.intent()) {
            case MOST_BOOKS_OWNER -> admin ? owners(understood, top) : adminOnly();
            case MOST_POPULAR_BOOK -> popular(understood, top);
            case TOP_EXPENSIVE -> {
                Table t = repo.books(f, admin, Sort.PRICE_DESC, top);
                yield result(p, understood, "Most expensive books", t,
                    rankedListSentence(t, p, admin, albanian, "most expensive", "më të shtrenjtë",
                        "priced books", "libra me çmim"));
            }
            case CHEAPEST -> {
                Table t = repo.books(f, admin, Sort.PRICE_ASC, top);
                yield result(p, understood, "Cheapest books", t,
                    rankedListSentence(t, p, admin, albanian, "cheapest", "më të lirë",
                        "priced books", "libra me çmim"));
            }
            case LONGEST -> {
                Table t = repo.books(f, admin, Sort.PAGES_DESC, top);
                yield result(p, understood, "Longest books", t,
                    rankedListSentence(t, p, admin, albanian, "longest", "më të gjatë",
                        "books with a page count", "libra me numër faqesh"));
            }
            case RECENT_BOOKS -> {
                Table t = repo.books(f, admin, Sort.NEWEST, top);
                yield result(p, understood, "Most recent books", t,
                    rankedListSentence(t, p, admin, albanian, "most recent", "më të fundit",
                        "books", "libra"));
            }
            case TOTAL_VALUE -> {
                Table t = repo.totalValue(f);
                List<Object> r = t.rows().get(0);
                yield result(p, understood, "Total value", t, totalValueSentence(
                    r.get(0), ((Number) r.get(1)).longValue(), p, admin, albanian));
            }
            case COUNT_BOOKS -> {
                Table t = repo.count(f);
                long count = ((Number) t.rows().get(0).get(0)).longValue();
                yield result(p, understood, "Book count", t, countSentence(count, p, admin, albanian));
            }
            case GENRE_BREAKDOWN -> {
                Table t = repo.genreBreakdown(f);
                yield result(p, understood, "Books per genre", t, genreBreakdownSentence(t, p, admin, albanian));
            }
            case LIST_BOOKS -> {
                Table t = repo.books(f, admin, Sort.NEWEST, limit == 0 ? MAX_ROWS : limit);
                yield result(p, understood, "Books", t,
                    rankedListSentence(t, p, admin, albanian, null, null, "books", "libra"));
            }
            case BOOKS_UNDER_PRICE -> {
                Table t = repo.booksUnderPrice(f, admin, p.price(), MAX_ROWS);
                yield result(p, understood, "Books under " + p.price(), t,
                    booksUnderPriceSentence(t, p, admin, albanian));
            }
            case AVERAGE_PRICE -> {
                Table t = repo.averagePrice(f);
                List<Object> r = t.rows().get(0);
                boolean noPricedBooks = ((Number) r.get(1)).longValue() == 0;
                yield result(p, understood, "Average price", t,
                    averagePriceSentence(r.get(0), ((Number) r.get(1)).longValue(), noPricedBooks, p, admin, albanian));
            }
            case TOP_AUTHOR -> {
                Table t = repo.topAuthor(f, top);
                yield result(p, understood, "Author with the most books", t, topAuthorSentence(t, p, admin, albanian));
            }
            case SEARCH_TITLE -> {
                Table t = repo.searchTitle(f, admin, p.searchText(), MAX_ROWS);
                yield result(p, understood, "Search results", t, searchTitleSentence(t, p, admin, albanian));
            }
            case UNKNOWN -> handleUnknown(question, genres, authors, scope, admin, albanian);
        };
    }

    /**
     * The legacy intent lookup gave up: try the general slot-based engine (any aggregate/field/
     * filter/groupBy combination {@link SpecParser} can recognize) before giving up for good.
     */
    private QueryResult handleUnknown(String question, List<String> genres, List<String> authors,
                                       UUID scope, boolean admin, boolean albanian) {
        QuerySpec spec = parser.toSpec(question, genres, authors);
        if (specHelper.isEmpty(spec)) {
            return unknownResult(admin, question);
        }
        if (spec.groupBy() == GroupBy.USER && !admin) {
            return adminOnly();
        }
        Table t = repo.run(sqlBuilder.build(spec, scope, admin));
        return genericResult(spec, admin, albanian, t);
    }

    private QueryResult genericResult(QuerySpec spec, boolean admin, boolean albanian, Table t) {
        String understood = summaryFormatter.understoodAs(spec, admin);
        String title = summaryFormatter.title(spec);
        String label = "QUERY_" + spec.aggregate()
            + (spec.field() != null ? "_" + spec.field() : "")
            + (spec.groupBy() != null ? "_BY_" + spec.groupBy() : "");
        String summary = spec.groupBy() != null
            ? summaryFormatter.groupSummary(spec, admin, albanian, t)
            : switch (spec.aggregate()) {
                case SUM, AVG, MIN, MAX -> {
                    List<Object> row = t.rows().get(0);
                    yield summaryFormatter.scalarSummary(spec, admin, albanian, row.get(0), ((Number) row.get(1)).longValue());
                }
                case COUNT -> summaryFormatter.countSummary(spec, admin, albanian, ((Number) t.rows().get(0).get(0)).longValue());
                case LIST -> summaryFormatter.listSummary(spec, admin, albanian, t);
                case GROUP_COUNT -> summaryFormatter.groupSummary(spec, admin, albanian, t);
            };
        return new QueryResult(label, understood, title, summary, t.columns(), t.rows(), false);
    }

    /** No spec signal at all: suggest a close example (edit-distance word overlap) when one exists. */
    private QueryResult unknownResult(boolean admin, String question) {
        List<String> pool = new ArrayList<>(COMMON_EXAMPLES);
        if (admin) pool.addAll(ADMIN_ONLY_EXAMPLES);
        List<String> qWords = Normalizer.words(Normalizer.normalize(question));
        List<String> ranked = rankByCloseness(qWords, pool);
        boolean hasMatch = !ranked.isEmpty() && wordOverlap(qWords, ranked.get(0)) >= 2;
        String summary = hasMatch
            ? "Did you mean: " + ranked.get(0) + "?"
            : "Try one of these questions.";
        List<String> picked = hasMatch ? ranked.subList(0, Math.min(3, ranked.size())) : pickExamples(admin, 3);
        return new QueryResult(Intent.UNKNOWN.name(), "I could not understand that question", "I did not understand that",
            summary, List.of("Example questions"), picked.stream().map(e -> List.<Object>of(e)).toList(), true);
    }

    /** Deterministic, stable-sorted by how many of the question's words fuzzy-match each example's words. */
    private List<String> rankByCloseness(List<String> qWords, List<String> pool) {
        List<String> sorted = new ArrayList<>(pool);
        sorted.sort((a, b) -> Integer.compare(wordOverlap(qWords, b), wordOverlap(qWords, a)));
        return sorted;
    }

    private int wordOverlap(List<String> qWords, String example) {
        List<String> exWords = Normalizer.words(Normalizer.normalize(example));
        int score = 0;
        for (String qw : qWords) {
            if (STOPWORDS.contains(qw)) continue;
            for (String ew : exWords) {
                if (STOPWORDS.contains(ew)) continue;
                if (EditDistance.closeEnough(qw, ew)) {
                    score++;
                    break;
                }
            }
        }
        return score;
    }

    private QueryResult owners(String understood, int top) {
        Table t = repo.topOwners(top);
        String summary = t.rows().isEmpty()
            ? "There are no users yet."
            : t.rows().get(0).get(0) + " owns the most books (" + t.rows().get(0).get(1) + ").";
        return new QueryResult(Intent.MOST_BOOKS_OWNER.name(), understood, "Users with the most books",
            summary, t.columns(), t.rows(), false);
    }

    private QueryResult popular(String understood, int top) {
        Table t = repo.popularBooks(top);
        String summary = t.rows().isEmpty()
            ? "There are no books yet."
            : "The most popular book is '" + t.rows().get(0).get(0) + "' by " + t.rows().get(0).get(1)
                + ", owned by " + t.rows().get(0).get(2) + " user(s).";
        return new QueryResult(Intent.MOST_POPULAR_BOOK.name(), understood, "Most popular books",
            summary, t.columns(), t.rows(), false);
    }

    /** A regular user asked a question that ranks/lists other users (admin-only). */
    private QueryResult adminOnly() {
        List<String> picked = pickExamples(false, 3);
        return new QueryResult(Intent.MOST_BOOKS_OWNER.name(), "Question about other users", "Not available",
            "This question is about other users, and only admins can see that. Try asking about your own books instead.",
            List.of("Example questions"), picked.stream().map(e -> List.<Object>of(e)).toList(), true);
    }

    private QueryResult result(Parsed p, String understood, String title, Table t, String summary) {
        return new QueryResult(p.intent().name(), understood, title, summary, t.columns(), t.rows(), false);
    }

    // ==================================================================================
    // Human-readable summary sentences. Filters are woven in as plain language (never
    // "status=COMPLETED"/"genre=X"), in English or Albanian depending on `albanian`
    // (see Vocabulary#isAlbanian). understoodAs() below is the one exception: it stays
    // in its original key=value shape because the frontend parses it (never displays it)
    // to build follow-up question chips.
    // ==================================================================================

    /** English trailing filter words, in reading order: status (unless COMPLETED, see {@link #enPrefix}), genre, author. */
    private List<String> enTrailingParts(Parsed p) {
        List<String> parts = new ArrayList<>();
        if (p.status() == BookStatus.READING) parts.add("currently reading");
        if (p.status() == BookStatus.TO_READ) parts.add("to read");
        if (p.genre() != null) parts.add("in " + SummaryFormatter.capitalize(p.genre()));
        if (p.author() != null) parts.add("by " + SummaryFormatter.titleCase(p.author()));
        return parts;
    }

    /** "completed " when that's the status filter (reads better before the noun), else "". */
    private String enPrefix(Parsed p) {
        return p.status() == BookStatus.COMPLETED ? "completed " : "";
    }

    /** Albanian trailing filter words: status, genre, author - Albanian adjectives follow the noun. */
    private List<String> alTrailingParts(Parsed p, boolean singularStatus) {
        List<String> parts = new ArrayList<>();
        if (p.status() != null) {
            String label = SummaryFormatter.statusLabel(p.status(), true);
            parts.add(singularStatus && label.endsWith("a") ? label.substring(0, label.length() - 1) : label);
        }
        if (p.genre() != null) parts.add("nga zhanri " + SummaryFormatter.capitalize(p.genre()));
        if (p.author() != null) parts.add("nga " + SummaryFormatter.titleCase(p.author()));
        return parts;
    }

    private String countSentence(long count, Parsed p, boolean admin, boolean albanian) {
        if (albanian) {
            if (count == 0) {
                String phrase = String.join(" dhe ", alTrailingParts(p, true));
                String suffix = phrase.isEmpty() ? "" : " " + phrase;
                return admin ? "Nuk ka asnjë libër" + suffix + " në bibliotekë." : "Nuk ke asnjë libër" + suffix + ".";
            }
            String phrase = String.join(" dhe ", alTrailingParts(p, false));
            String noun = count == 1 ? "libër" : "libra";
            String base = (admin ? "Ka " : "Ke ") + count + " " + noun + (phrase.isEmpty() ? "" : " " + phrase);
            return admin ? base + " në bibliotekë." : base + ".";
        }
        String prefix = enPrefix(p);
        List<String> trailing = enTrailingParts(p);
        if (count == 0 && prefix.isEmpty() && trailing.isEmpty()) {
            return admin ? "There are no books in the library yet." : "You don't have any books yet.";
        }
        String trailingPhrase = trailing.isEmpty() ? "" : " " + String.join(" and ", trailing);
        String noun = count == 1 ? "book" : "books";
        return admin
            ? "There are " + count + " " + prefix + noun + trailingPhrase + " in the library."
            : "You have " + count + " " + prefix + noun + trailingPhrase + ".";
    }

    /** Shared by TOP_EXPENSIVE/CHEAPEST/LONGEST/RECENT_BOOKS/LIST_BOOKS: narrate the table, not one row of it. */
    private String rankedListSentence(Table t, Parsed p, boolean admin, boolean albanian,
                                       String enAdjective, String alAdjective, String emptyNounEn, String emptyNounAl) {
        if (t.rows().isEmpty()) {
            if (albanian) {
                String phrase = String.join(" dhe ", alTrailingParts(p, true));
                String noun = phrase.isEmpty() ? emptyNounAl : emptyNounAl + " " + phrase;
                return admin ? "Nuk ka " + noun + " në bibliotekë." : "Nuk ke " + noun + ".";
            }
            List<String> trailing = enTrailingParts(p);
            String prefix = enPrefix(p);
            String noun = (prefix + emptyNounEn).trim();
            if (!trailing.isEmpty()) noun = noun + " " + String.join(" and ", trailing);
            return admin ? "There are no " + noun + " in the library yet." : "You don't have any " + noun + " yet.";
        }
        int count = t.rows().size();
        if (albanian) {
            String phrase = String.join(" dhe ", alTrailingParts(p, false));
            String noun = count == 1 ? "libri" : "librat";
            String adjPart = alAdjective == null ? "" : " " + alAdjective;
            String scope = admin ? " në bibliotekë" : " e tu";
            String filterPart = phrase.isEmpty() ? "" : " " + phrase;
            return count + " " + noun + adjPart + scope + filterPart + " janë më poshtë.";
        }
        String prefix = enPrefix(p);
        List<String> trailing = enTrailingParts(p);
        String trailingPhrase = trailing.isEmpty() ? "" : " " + String.join(" and ", trailing);
        String noun = count == 1 ? "book" : "books";
        String adjPart = enAdjective == null ? "" : " " + enAdjective;
        String who = admin ? "The" : "Your";
        String where = admin ? " in the library" : "";
        return who + " " + count + adjPart + " " + prefix + noun + trailingPhrase + where + " are shown below.";
    }

    private String totalValueSentence(Object total, long count, Parsed p, boolean admin, boolean albanian) {
        if (albanian) {
            String phrase = String.join(" dhe ", alTrailingParts(p, count == 0));
            String suffix = phrase.isEmpty() ? "" : " " + phrase;
            if (count == 0) {
                return admin ? "Nuk ka asnjë libër" + suffix + " në bibliotekë." : "Nuk ke asnjë libër" + suffix + ".";
            }
            String scope = admin ? "në bibliotekë" : "librat e tu";
            return "Vlera totale e " + scope + suffix + ": " + total + " nga " + SummaryFormatter.pluralizeAl(count, "libër", "libra") + ".";
        }
        String prefix = enPrefix(p);
        List<String> trailing = enTrailingParts(p);
        String trailingPhrase = trailing.isEmpty() ? "" : " " + String.join(" and ", trailing);
        if (count == 0) {
            return admin
                ? "There are no " + prefix + "books" + trailingPhrase + " in the library yet."
                : "You don't have any " + prefix + "books" + trailingPhrase + " yet.";
        }
        String where = admin ? "in the whole library" : "in your library";
        return "Total value " + where + trailingPhrase + ": " + total + " across " + SummaryFormatter.pluralize(count, "book") + ".";
    }

    private String genreBreakdownSentence(Table t, Parsed p, boolean admin, boolean albanian) {
        int count = t.rows().size();
        if (albanian) {
            String phrase = String.join(" dhe ", alTrailingParts(p, count == 0));
            if (count == 0) {
                String suffix = phrase.isEmpty() ? "" : " " + phrase;
                return admin ? "Nuk ka asnjë libër" + suffix + " në bibliotekë." : "Nuk ke asnjë libër" + suffix + ".";
            }
            String scope = admin ? "në bibliotekë" : "te librat e tu";
            String suffix = phrase.isEmpty() ? "" : " " + phrase;
            return "Ka " + SummaryFormatter.pluralizeAl(count, "zhanër", "zhanre") + suffix + " " + scope + ".";
        }
        List<String> trailing = enTrailingParts(p);
        String trailingPhrase = trailing.isEmpty() ? "" : " " + String.join(" and ", trailing);
        if (count == 0) {
            return admin ? "There are no books in the library yet." : "You don't have any books yet.";
        }
        String where = admin ? "in the whole library" : "in your library";
        return SummaryFormatter.pluralize(count, "genre") + " " + where + trailingPhrase + ".";
    }

    private String booksUnderPriceSentence(Table t, Parsed p, boolean admin, boolean albanian) {
        int count = t.rows().size();
        if (albanian) {
            if (count == 0) {
                return admin
                    ? "Nuk ka libra me çmim deri në $" + p.price() + " në bibliotekë."
                    : "Nuk ke libra me çmim deri në $" + p.price() + ".";
            }
            String scope = admin ? "në bibliotekë" : "që ke";
            return SummaryFormatter.pluralizeAl(count, "libër", "libra") + " me çmim deri në $" + p.price() + " " + scope + ".";
        }
        String where = admin ? "in the whole library" : "in your library";
        if (count == 0) {
            return "No books priced at or under $" + p.price() + " found " + where + ".";
        }
        return "Found " + SummaryFormatter.pluralize(count, "book") + " priced at or under $" + p.price() + " " + where + ".";
    }

    private String averagePriceSentence(Object average, long count, boolean noPricedBooks, Parsed p, boolean admin, boolean albanian) {
        if (albanian) {
            if (noPricedBooks) return admin ? "Ende nuk ka libra me çmim në bibliotekë." : "Ende nuk ke asnjë libër me çmim.";
            String scope = admin ? "në bibliotekë" : "e librave të tu";
            return "Çmimi mesatar " + scope + ": " + average + " nga " + SummaryFormatter.pluralizeAl(count, "libër", "libra") + " me çmim.";
        }
        if (noPricedBooks) return "You don't have any books with a price yet.";
        String where = admin ? "in the whole library" : "in your library";
        return "Average price " + where + ": " + average + " across " + SummaryFormatter.pluralize(count, "priced book") + ".";
    }

    private String topAuthorSentence(Table t, Parsed p, boolean admin, boolean albanian) {
        if (t.rows().isEmpty()) {
            if (albanian) return admin ? "Nuk ka ende libra në bibliotekë." : "Nuk ke ende asnjë libër.";
            return admin ? "There are no books in the library yet." : "You don't have any books yet.";
        }
        Object author = t.rows().get(0).get(0);
        Object count = t.rows().get(0).get(1);
        if (albanian) {
            String scope = admin ? "në bibliotekë" : "te librat e tu";
            return author + " ka më shumë libra " + scope + " (" + count + ").";
        }
        String where = admin ? "in the whole library" : "in your library";
        return author + " has the most books " + where + " (" + count + ").";
    }

    private String searchTitleSentence(Table t, Parsed p, boolean admin, boolean albanian) {
        if (albanian) {
            String scope = admin ? "në bibliotekë" : "te librat e tu";
            return t.rows().isEmpty()
                ? "Nuk u gjet asnjë libër me titull që përmban \"" + p.searchText() + "\" " + scope + "."
                : SummaryFormatter.pluralizeAl(t.rows().size(), "libër", "libra") + " me titull që përmban \""
                    + p.searchText() + "\" " + scope + ".";
        }
        String where = admin ? "in the whole library" : "in your library";
        return t.rows().isEmpty()
            ? "No books with a title matching '" + p.searchText() + "' found " + where + "."
            : "Found " + SummaryFormatter.pluralize(t.rows().size(), "book") + " matching '" + p.searchText() + "' " + where + ".";
    }

    /** Short readable sentence describing how the question was interpreted. Part of the API contract:
     *  the frontend parses this for "genre="/"author="/"status=" to build follow-up chips, so its
     *  key=value shape must not change - it is never shown to the user. */
    private String understoodAs(Parsed p, boolean admin) {
        String sentence = switch (p.intent()) {
            case MOST_BOOKS_OWNER -> "Who owns the most books";
            case MOST_POPULAR_BOOK -> "The most popular book(s)";
            case TOP_EXPENSIVE -> "Most expensive books";
            case CHEAPEST -> "Cheapest books";
            case LONGEST -> "Longest books by page count";
            case TOTAL_VALUE -> "Total value of books";
            case COUNT_BOOKS -> "How many books";
            case GENRE_BREAKDOWN -> "Books grouped by genre";
            case LIST_BOOKS -> "A list of books";
            case BOOKS_UNDER_PRICE -> "Books priced at or under " + p.price();
            case AVERAGE_PRICE -> "The average book price";
            case TOP_AUTHOR -> "The author with the most books";
            case RECENT_BOOKS -> "The most recently added books";
            case SEARCH_TITLE -> "Books with a title matching \"" + p.searchText() + "\"";
            case UNKNOWN -> "I could not understand that question";
        };
        if (p.intent() == Intent.UNKNOWN) return sentence;
        StringBuilder sb = new StringBuilder(sentence)
            .append(" (").append(admin ? "library-wide" : "in your books").append(")");
        if (p.genre() != null) sb.append(", genre=").append(p.genre());
        if (p.author() != null) sb.append(", author=").append(p.author());
        if (p.status() != null) sb.append(", status=").append(p.status());
        if (p.limit() != null && p.intent() != Intent.BOOKS_UNDER_PRICE) sb.append(", limit=").append(p.limit());
        return sb.toString();
    }
}
