package com.fjonna.library.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fjonna.library.TestcontainersConfiguration;
import com.fjonna.library.book.Book;
import com.fjonna.library.book.BookRepository;
import com.fjonna.library.book.BookStatus;
import com.fjonna.library.user.User;
import com.fjonna.library.user.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class RecommendationServiceTest {

    @Autowired
    private RecommendationService recommendationService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BookRepository bookRepository;

    private User user(String username) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setUsername(username);
        return userRepository.save(u);
    }

    private void book(User owner, String title, String author, String genre, BookStatus status) {
        Book b = new Book();
        b.setUser(owner);
        b.setTitle(title);
        b.setAuthor(author);
        b.setGenre(genre);
        b.setStatus(status);
        bookRepository.save(b);
    }

    @Test
    void recommendsByGenreAuthorAndSimilarReaders() {
        User me = user("me");
        User other = user("other");
        User twin = user("twin");

        book(me, "Fellowship", "Tolkien", "Fantasy", BookStatus.COMPLETED);
        book(me, "Two Towers", "Tolkien", "Fantasy", BookStatus.TO_READ);

        book(other, "Mistborn", "Sanderson", "Fantasy", BookStatus.TO_READ);
        book(other, "Return of the King", "Tolkien", "Fantasy", BookStatus.TO_READ);

        book(twin, "Fellowship", "Tolkien", "Fantasy", BookStatus.COMPLETED);
        book(twin, "Two Towers", "Tolkien", "Fantasy", BookStatus.TO_READ);
        book(twin, "Mistake", "Someone", "Horror", BookStatus.TO_READ);

        List<RecommendationItem> result = recommendationService.recommend(me.getId());

        assertThat(result).extracting(RecommendationItem::title)
            .containsExactly("Return of the King", "Mistborn", "Mistake");
        assertThat(result.get(0).reason()).isEqualTo("Because you read a lot of Fantasy");
        assertThat(result.get(2).reason()).isEqualTo("Readers with similar books also have this");
    }

    @Test
    void fallsBackToPopularWhenUserHasNoBooks() {
        User a = user("a");
        User b = user("b");
        User me = user("me-empty");

        book(a, "Dune", "Herbert", "Sci-Fi", BookStatus.COMPLETED);
        book(b, "Dune", "Herbert", "Sci-Fi", BookStatus.TO_READ);
        book(a, "Rare Book", "Nobody", "Drama", BookStatus.TO_READ);

        List<RecommendationItem> result = recommendationService.recommend(me.getId());

        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(r -> r.reason().equals("Popular in the library"));
        assertThat(result.get(0).title()).isEqualTo("Dune");
    }
}
