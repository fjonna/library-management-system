package com.fjonna.library.agent;

import com.fjonna.library.agent.AgentRepository.Filter;
import com.fjonna.library.agent.AgentRepository.Sort;
import com.fjonna.library.agent.AgentRepository.Table;
import com.fjonna.library.agent.QueryParser.Parsed;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AgentService {

    private static final int MAX_ROWS = 50;

    public static final List<String> EXAMPLES = List.of(
        "Who owns the most books?",
        "Which is the most popular book?",
        "Show the five most expensive books",
        "Show my cheapest 3 books",
        "How many books have I completed?",
        "Show books I am currently reading",
        "How many books per genre?",
        "What is the total value of my books?",
        "Show my longest books");

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

        return switch (p.intent()) {
            case MOST_BOOKS_OWNER -> admin ? owners(top) : adminOnly();
            case MOST_POPULAR_BOOK -> popular(top);
            case TOP_EXPENSIVE -> {
                Table t = repo.books(f, admin, Sort.PRICE_DESC, top);
                yield result(p, "Most expensive books", t, t.rows().isEmpty()
                    ? "No priced books found " + where + "."
                    : "The most expensive book " + where + " is '" + t.rows().get(0).get(1)
                        + "' at " + t.rows().get(0).get(5) + "." + filters(p));
            }
            case CHEAPEST -> {
                Table t = repo.books(f, admin, Sort.PRICE_ASC, top);
                yield result(p, "Cheapest books", t, t.rows().isEmpty()
                    ? "No priced books found " + where + "."
                    : "The cheapest book " + where + " is '" + t.rows().get(0).get(1)
                        + "' at " + t.rows().get(0).get(5) + "." + filters(p));
            }
            case LONGEST -> {
                Table t = repo.books(f, admin, Sort.PAGES_DESC, top);
                yield result(p, "Longest books", t, t.rows().isEmpty()
                    ? "No books with page counts found " + where + "."
                    : "The longest book " + where + " is '" + t.rows().get(0).get(1)
                        + "' with " + t.rows().get(0).get(6) + " pages." + filters(p));
            }
            case TOTAL_VALUE -> {
                Table t = repo.totalValue(f);
                List<Object> r = t.rows().get(0);
                yield result(p, "Total value", t,
                    "Total value " + where + ": " + r.get(0) + " across " + r.get(1) + " book(s)." + filters(p));
            }
            case COUNT_BOOKS -> {
                Table t = repo.count(f);
                yield result(p, "Book count", t,
                    ((Number) t.rows().get(0).get(0)).longValue() + " book(s) " + where + "." + filters(p));
            }
            case GENRE_BREAKDOWN -> {
                Table t = repo.genreBreakdown(f);
                yield result(p, "Books per genre", t,
                    t.rows().size() + " genre(s) " + where + "." + filters(p));
            }
            case LIST_BOOKS -> {
                Table t = repo.books(f, admin, Sort.NEWEST, limit == 0 ? MAX_ROWS : limit);
                yield result(p, "Books", t, t.rows().isEmpty()
                    ? "No books matched " + where + "." + filters(p)
                    : "Found " + t.rows().size() + " book(s) " + where + "." + filters(p));
            }
            case UNKNOWN -> new QueryResult(p.intent().name(), "I did not understand that",
                "Try one of these questions.", List.of("Example questions"),
                EXAMPLES.stream().map(e -> List.<Object>of(e)).toList());
        };
    }

    private QueryResult owners(int top) {
        Table t = repo.topOwners(top);
        String summary = t.rows().isEmpty()
            ? "There are no users yet."
            : t.rows().get(0).get(0) + " owns the most books (" + t.rows().get(0).get(1) + ").";
        return new QueryResult(Intent.MOST_BOOKS_OWNER.name(), "Users with the most books",
            summary, t.columns(), t.rows());
    }

    private QueryResult popular(int top) {
        Table t = repo.popularBooks(top);
        String summary = t.rows().isEmpty()
            ? "There are no books yet."
            : "The most popular book is '" + t.rows().get(0).get(0) + "' by " + t.rows().get(0).get(1)
                + ", owned by " + t.rows().get(0).get(2) + " user(s).";
        return new QueryResult(Intent.MOST_POPULAR_BOOK.name(), "Most popular books",
            summary, t.columns(), t.rows());
    }

    private QueryResult adminOnly() {
        return new QueryResult(Intent.MOST_BOOKS_OWNER.name(), "Admins only",
            "Questions about other users are only available to admins.", List.of(), List.of());
    }

    private QueryResult result(Parsed p, String title, Table t, String summary) {
        return new QueryResult(p.intent().name(), title, summary, t.columns(), t.rows());
    }

    private String filters(Parsed p) {
        StringBuilder sb = new StringBuilder();
        if (p.genre() != null) sb.append(" genre=").append(p.genre());
        if (p.author() != null) sb.append(" author=").append(p.author());
        if (p.status() != null) sb.append(" status=").append(p.status());
        return sb.isEmpty() ? "" : " Filters:" + sb + ".";
    }
}
