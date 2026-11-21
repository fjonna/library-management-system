package com.fjonna.library.recommendation;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Fixed, parameterized queries only. No SQL, no ML: plain counting and matching. */
@Repository
@RequiredArgsConstructor
public class RecommendationRepository {

    private static final int TOP_LABELS = 5;

    private final JdbcClient jdbc;

    public record WeightedLabel(String label, long weight) {
    }

    public record BookMatch(String title, String author, String genre) {
    }

    public record CollabBook(String title, String author, String genre, UUID ownerId) {
    }

    public record SimilarUser(UUID userId, long shared) {
    }

    /** Genres the user owns the most of; completed books weigh double. */
    public List<WeightedLabel> topGenres(UUID uid) {
        return jdbc.sql("select lower(genre) as label, sum(case when status = 'COMPLETED' then 2 else 1 end) as weight"
                + " from books where user_id = :uid and genre is not null"
                + " group by lower(genre) order by weight desc, label limit :lim")
            .param("uid", uid).param("lim", TOP_LABELS)
            .query(WeightedLabel.class).list();
    }

    /** Authors the user owns the most of; completed books weigh double. */
    public List<WeightedLabel> topAuthors(UUID uid) {
        return jdbc.sql("select lower(author) as label, sum(case when status = 'COMPLETED' then 2 else 1 end) as weight"
                + " from books where user_id = :uid"
                + " group by lower(author) order by weight desc, label limit :lim")
            .param("uid", uid).param("lim", TOP_LABELS)
            .query(WeightedLabel.class).list();
    }

    /** Books owned by other users in one of the given genres, that the user does not already own (by title+author). */
    public List<BookMatch> booksByGenres(UUID uid, List<String> genres) {
        if (genres.isEmpty()) return List.of();
        return jdbc.sql("select min(b.title) as title, min(b.author) as author, min(b.genre) as genre"
                + " from books b"
                + " where b.user_id <> :uid and lower(b.genre) in (:genres)"
                + " and not exists (select 1 from books m where m.user_id = :uid"
                + "   and lower(m.title) = lower(b.title) and lower(m.author) = lower(b.author))"
                + " group by lower(b.title), lower(b.author)")
            .param("uid", uid).param("genres", genres)
            .query(BookMatch.class).list();
    }

    /** Books owned by other users written by one of the given authors, that the user does not already own. */
    public List<BookMatch> booksByAuthors(UUID uid, List<String> authors) {
        if (authors.isEmpty()) return List.of();
        return jdbc.sql("select min(b.title) as title, min(b.author) as author, min(b.genre) as genre"
                + " from books b"
                + " where b.user_id <> :uid and lower(b.author) in (:authors)"
                + " and not exists (select 1 from books m where m.user_id = :uid"
                + "   and lower(m.title) = lower(b.title) and lower(m.author) = lower(b.author))"
                + " group by lower(b.title), lower(b.author)")
            .param("uid", uid).param("authors", authors)
            .query(BookMatch.class).list();
    }

    /** Other users who own at least {@code minShared} of the same title+author pairs as the user. */
    public List<SimilarUser> similarUsers(UUID uid, int minShared) {
        return jdbc.sql("select b2.user_id as userId, count(*) as shared"
                + " from books b1 join books b2"
                + "   on lower(b1.title) = lower(b2.title) and lower(b1.author) = lower(b2.author)"
                + "   and b2.user_id <> b1.user_id"
                + " where b1.user_id = :uid"
                + " group by b2.user_id having count(*) >= :minShared"
                + " order by shared desc")
            .param("uid", uid).param("minShared", minShared)
            .query(SimilarUser.class).list();
    }

    /** Books owned by the given users, that the user does not already own (by title+author). */
    public List<CollabBook> booksOwnedBy(UUID uid, List<UUID> ownerIds) {
        if (ownerIds.isEmpty()) return List.of();
        return jdbc.sql("select b.title as title, b.author as author, b.genre as genre, b.user_id as ownerId"
                + " from books b"
                + " where b.user_id in (:ownerIds)"
                + " and not exists (select 1 from books m where m.user_id = :uid"
                + "   and lower(m.title) = lower(b.title) and lower(m.author) = lower(b.author))")
            .param("uid", uid).param("ownerIds", ownerIds)
            .query(CollabBook.class).list();
    }

    /** Most popular books overall (owned by the most distinct users); used when a user owns no books. */
    public List<BookMatch> mostPopular(int limit) {
        return jdbc.sql("select min(b.title) as title, min(b.author) as author, min(b.genre) as genre"
                + " from books b"
                + " group by lower(trim(b.title)), lower(trim(b.author))"
                + " order by count(distinct b.user_id) desc, title limit :lim")
            .param("lim", limit)
            .query(BookMatch.class).list();
    }
}
