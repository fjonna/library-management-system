package com.fjonna.library;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Flyway runs automatically on context startup (spring.flyway.enabled=true), against a clean
 * Testcontainers Postgres, so simply starting the context here proves V2__add_indexes.sql applies
 * cleanly. We also check the specific indexes it's supposed to create actually exist.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MigrationIndexesTest {

    @Autowired
    private JdbcClient jdbc;

    @Test
    void v2MigrationCreatesTheExpectedIndexesOnBooks() {
        List<String> indexNames = jdbc.sql("select indexname from pg_indexes where tablename = 'books'")
            .query(String.class)
            .list();

        assertThat(indexNames).contains(
            "idx_books_user_id",
            "idx_books_genre",
            "idx_books_user_id_genre",
            "idx_books_user_id_status",
            "idx_books_user_id_created_at",
            "idx_books_genre_lower",
            "idx_books_author_lower",
            "idx_books_title_lower",
            "idx_books_title_author_lower");
    }
}
