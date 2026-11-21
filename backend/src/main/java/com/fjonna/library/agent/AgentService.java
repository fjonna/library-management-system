package com.fjonna.library.agent;

import com.fjonna.library.agent.AgentRepository.Filter;
import com.fjonna.library.agent.AgentRepository.Sort;
import com.fjonna.library.agent.AgentRepository.Table;
import com.fjonna.library.agent.QueryParser.Parsed;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AgentService {

    private static final int MAX_ROWS = 50;
    private static final Random RANDOM = new Random();

    public static final List<String> EXAMPLES = List.of(
        "Who owns the most books?",
        "Which is the most popular book?",
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
        "Kush ka më shumë libra?",
        "Cilat janë librat më të shtrenjtë?",
        "Sa libra kam?",
        "Librat më të fundit",
        "Kërko librin me titullin Dune");

    private final QueryParser parser;
    private final AgentRepository repo;

    @Transactional(readOnly = true)
    public QueryResult answer(UUID userId, boolean admin, String question) {
        UUID scope = admin ? null : userId;
        Parsed p = parser.parse(question, repo.genres(scope), repo.authors(scope));
        Filter f = new Filter(scope, p.genre(), p.author(), p.status());
        int limit = p.limit() == null ? 0 : Math.min(Math.max(p.limit(), 1), MAX_ROWS);
        int top = limit == 0 ? 5 : limit;
        String where = admin ? "in the whole library" : "in your library";
        String understood = understoodAs(p, admin);

        return switch (p.intent()) {
            case MOST_BOOKS_OWNER -> admin ? owners(understood, top) : adminOnly(understood);
            case MOST_POPULAR_BOOK -> popular(understood, top);
            case TOP_EXPENSIVE -> {
                Table t = repo.books(f, admin, Sort.PRICE_DESC, top);
                yield result(p, understood, "Most expensive books", t, t.rows().isEmpty()
                    ? "No priced books found " + where + "."
                    : "The most expensive book " + where + " is '" + t.rows().get(0).get(1)
                        + "' at " + t.rows().get(0).get(5) + "." + filters(p));
            }
            case CHEAPEST -> {
                Table t = repo.books(f, admin, Sort.PRICE_ASC, top);
                yield result(p, understood, "Cheapest books", t, t.rows().isEmpty()
                    ? "No priced books found " + where + "."
                    : "The cheapest book " + where + " is '" + t.rows().get(0).get(1)
                        + "' at " + t.rows().get(0).get(5) + "." + filters(p));
            }
            case LONGEST -> {
                Table t = repo.books(f, admin, Sort.PAGES_DESC, top);
                yield result(p, understood, "Longest books", t, t.rows().isEmpty()
                    ? "No books with page counts found " + where + "."
                    : "The longest book " + where + " is '" + t.rows().get(0).get(1)
                        + "' with " + t.rows().get(0).get(6) + " pages." + filters(p));
            }
            case TOTAL_VALUE -> {
                Table t = repo.totalValue(f);
                List<Object> r = t.rows().get(0);
                yield result(p, understood, "Total value", t,
                    "Total value " + where + ": " + r.get(0) + " across " + r.get(1) + " book(s)." + filters(p));
            }
            case COUNT_BOOKS -> {
                Table t = repo.count(f);
                yield result(p, understood, "Book count", t,
                    ((Number) t.rows().get(0).get(0)).longValue() + " book(s) " + where + "." + filters(p));
            }
            case GENRE_BREAKDOWN -> {
                Table t = repo.genreBreakdown(f);
                yield result(p, understood, "Books per genre", t,
                    t.rows().size() + " genre(s) " + where + "." + filters(p));
            }
            case LIST_BOOKS -> {
                Table t = repo.books(f, admin, Sort.NEWEST, limit == 0 ? MAX_ROWS : limit);
                yield result(p, understood, "Books", t, t.rows().isEmpty()
                    ? "No books matched " + where + "." + filters(p)
                    : "Found " + t.rows().size() + " book(s) " + where + "." + filters(p));
            }
            case BOOKS_UNDER_PRICE -> {
                Table t = repo.booksUnderPrice(f, admin, p.price(), MAX_ROWS);
                yield result(p, understood, "Books under " + p.price(), t, t.rows().isEmpty()
                    ? "No books priced at or under " + p.price() + " found " + where + "." + filters(p)
                    : "Found " + t.rows().size() + " book(s) at or under " + p.price() + " " + where + "." + filters(p));
            }
            case AVERAGE_PRICE -> {
                Table t = repo.averagePrice(f);
                List<Object> r = t.rows().get(0);
                yield result(p, understood, "Average price", t,
                    "Average price " + where + ": " + r.get(0) + " across " + r.get(1) + " priced book(s)." + filters(p));
            }
            case TOP_AUTHOR -> {
                Table t = repo.topAuthor(f, top);
                yield result(p, understood, "Author with the most books", t, t.rows().isEmpty()
                    ? "No books found " + where + "."
                    : t.rows().get(0).get(0) + " has the most books " + where + " (" + t.rows().get(0).get(1) + ")." + filters(p));
            }
            case RECENT_BOOKS -> {
                Table t = repo.books(f, admin, Sort.NEWEST, top);
                yield result(p, understood, "Most recent books", t, t.rows().isEmpty()
                    ? "No books found " + where + "."
                    : "Found " + t.rows().size() + " recent book(s) " + where + "." + filters(p));
            }
            case SEARCH_TITLE -> {
                Table t = repo.searchTitle(f, admin, p.searchText(), MAX_ROWS);
                yield result(p, understood, "Search results", t, t.rows().isEmpty()
                    ? "No books with a title matching '" + p.searchText() + "' found " + where + "."
                    : "Found " + t.rows().size() + " book(s) matching '" + p.searchText() + "' " + where + ".");
            }
            case UNKNOWN -> {
                List<String> suggestions = new java.util.ArrayList<>(EXAMPLES);
                Collections.shuffle(suggestions, RANDOM);
                List<String> picked = suggestions.subList(0, Math.min(3, suggestions.size()));
                yield new QueryResult(p.intent().name(), understood, "I did not understand that",
                    "Try one of these questions.", List.of("Example questions"),
                    picked.stream().map(e -> List.<Object>of(e)).toList());
            }
        };
    }

    private QueryResult owners(String understood, int top) {
        Table t = repo.topOwners(top);
        String summary = t.rows().isEmpty()
            ? "There are no users yet."
            : t.rows().get(0).get(0) + " owns the most books (" + t.rows().get(0).get(1) + ").";
        return new QueryResult(Intent.MOST_BOOKS_OWNER.name(), understood, "Users with the most books",
            summary, t.columns(), t.rows());
    }

    private QueryResult popular(String understood, int top) {
        Table t = repo.popularBooks(top);
        String summary = t.rows().isEmpty()
            ? "There are no books yet."
            : "The most popular book is '" + t.rows().get(0).get(0) + "' by " + t.rows().get(0).get(1)
                + ", owned by " + t.rows().get(0).get(2) + " user(s).";
        return new QueryResult(Intent.MOST_POPULAR_BOOK.name(), understood, "Most popular books",
            summary, t.columns(), t.rows());
    }

    private QueryResult adminOnly(String understood) {
        return new QueryResult(Intent.MOST_BOOKS_OWNER.name(), understood, "Admins only",
            "Questions about other users are only available to admins.", List.of(), List.of());
    }

    private QueryResult result(Parsed p, String understood, String title, Table t, String summary) {
        return new QueryResult(p.intent().name(), understood, title, summary, t.columns(), t.rows());
    }

    private String filters(Parsed p) {
        StringBuilder sb = new StringBuilder();
        if (p.genre() != null) sb.append(" genre=").append(p.genre());
        if (p.author() != null) sb.append(" author=").append(p.author());
        if (p.status() != null) sb.append(" status=").append(p.status());
        return sb.isEmpty() ? "" : " Filters:" + sb + ".";
    }

    /** Short readable sentence describing how the question was interpreted. */
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
