package com.fjonna.library.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.fjonna.library.TestcontainersConfiguration;
import com.fjonna.library.agent.AgentRepository.Filter;
import com.fjonna.library.agent.AgentRepository.Table;
import com.fjonna.library.book.Book;
import com.fjonna.library.book.BookRepository;
import com.fjonna.library.book.BookStatus;
import com.fjonna.library.user.User;
import com.fjonna.library.user.UserRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * Real-SQL regression tests for a user with zero books. Both bugs covered
 * here were found by actually calling the running app as a brand-new,
 * zero-books user and reading the backend's stack trace / response body:
 *   - topAuthor() built SQL with a ":lim" placeholder but never bound it,
 *     throwing InvalidDataAccessApiUsageException (500) for ANY user, not
 *     just one with no books.
 *   - averagePrice() did "avg(b.price)" with no coalesce, so with zero
 *     priced books the aggregate is SQL NULL and leaked the literal string
 *     "null" into the summary sentence.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class AgentRepositoryTest {

    @Autowired
    private AgentRepository repo;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private SqlBuilder sqlBuilder;

    private User user(String username) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setUsername(username);
        return userRepository.save(u);
    }

    private void book(User owner, String title, String author, String genre, BookStatus status,
                       BigDecimal price, Integer pages) {
        Book b = new Book();
        b.setUser(owner);
        b.setTitle(title);
        b.setAuthor(author);
        b.setGenre(genre);
        b.setStatus(status);
        b.setPrice(price);
        b.setPages(pages);
        bookRepository.save(b);
    }

    @Test
    void topAuthorDoesNotThrowAndReturnsEmptyTableForUserWithNoBooks() {
        User me = user("empty-topauthor");

        Table t = repo.topAuthor(new Filter(me.getId(), null, null, null), 5);

        assertThat(t.rows()).isEmpty();
    }

    @Test
    void averagePriceNeverReturnsNullForUserWithNoBooks() {
        User me = user("empty-avgprice");

        Table t = repo.averagePrice(new Filter(me.getId(), null, null, null));

        assertThat(t.rows()).hasSize(1);
        assertThat(t.rows().get(0).get(0)).isNotNull();
        assertThat(((Number) t.rows().get(0).get(1)).longValue()).isZero();
    }

    // ---- generic engine (QuerySpec -> SqlBuilder -> AgentRepository.run) against real SQL -------------

    @Test
    void sumOfPagesAcrossScopedBooks() {
        User me = user("spec-sum-pages");
        book(me, "Dune", "Frank Herbert", "sci-fi", BookStatus.TO_READ, new BigDecimal("12.00"), 600);
        book(me, "Children of Dune", "Frank Herbert", "sci-fi", BookStatus.TO_READ, new BigDecimal("10.00"), 400);

        QuerySpec spec = new QuerySpec(Aggregate.SUM, Field.PAGES, QuerySpec.Filters.NONE, null, null, null);
        Table t = repo.run(sqlBuilder.build(spec, me.getId(), false));

        assertThat(((Number) t.rows().get(0).get(0)).longValue()).isEqualTo(1000L);
        assertThat(((Number) t.rows().get(0).get(1)).longValue()).isEqualTo(2L);
    }

    @Test
    void pagesBetweenFilterNarrowsResults() {
        User me = user("spec-pages-between");
        book(me, "Short", "A", "fiction", BookStatus.TO_READ, null, 100);
        book(me, "Medium", "A", "fiction", BookStatus.TO_READ, null, 250);
        book(me, "Long", "A", "fiction", BookStatus.TO_READ, null, 900);

        QuerySpec.Filters f = new QuerySpec.Filters(null, null, null, null, null, null, 200, 500);
        QuerySpec spec = new QuerySpec(Aggregate.LIST, null, f, null, null, 10);
        Table t = repo.run(sqlBuilder.build(spec, me.getId(), false));

        assertThat(t.rows()).hasSize(1);
        assertThat(t.rows().get(0).get(1)).isEqualTo("Medium");
    }

    @Test
    void groupByUserCountIncludesUsersWithZeroBooks() {
        User withBooks = user("spec-group-with-books");
        User withoutBooks = user("spec-group-without-books");
        book(withBooks, "Dune", "Frank Herbert", "sci-fi", BookStatus.TO_READ, null, null);

        QuerySpec spec = new QuerySpec(Aggregate.GROUP_COUNT, null, QuerySpec.Filters.NONE, GroupBy.USER, null, 50);
        Table t = repo.run(sqlBuilder.build(spec, null, true));

        assertThat(t.rows()).anySatisfy(row -> {
            assertThat(row.get(0)).isEqualTo(withoutBooks.getUsername());
            assertThat(((Number) row.get(1)).longValue()).isZero();
        });
    }
}
