package com.fjonna.library.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.fjonna.library.TestcontainersConfiguration;
import com.fjonna.library.agent.AgentRepository.Filter;
import com.fjonna.library.agent.AgentRepository.Table;
import com.fjonna.library.user.User;
import com.fjonna.library.user.UserRepository;
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

    private User user(String username) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setUsername(username);
        return userRepository.save(u);
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
}
