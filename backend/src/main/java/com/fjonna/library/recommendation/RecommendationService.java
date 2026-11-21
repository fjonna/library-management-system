package com.fjonna.library.recommendation;

import com.fjonna.library.recommendation.RecommendationRepository.BookMatch;
import com.fjonna.library.recommendation.RecommendationRepository.CollabBook;
import com.fjonna.library.recommendation.RecommendationRepository.SimilarUser;
import com.fjonna.library.recommendation.RecommendationRepository.WeightedLabel;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int MAX_RESULTS = 10;
    private static final int MIN_SHARED_BOOKS = 2;

    private final RecommendationRepository repo;

    private record Scored(String title, String author, String genre, String reason, double score) {
    }

    @Transactional(readOnly = true)
    public List<RecommendationItem> recommend(UUID userId) {
        List<WeightedLabel> genres = repo.topGenres(userId);
        List<WeightedLabel> authors = repo.topAuthors(userId);

        if (authors.isEmpty()) {
            return repo.mostPopular(MAX_RESULTS).stream()
                .map(b -> new RecommendationItem(b.title(), b.author(), b.genre(), "Popular in the library"))
                .toList();
        }

        Map<String, Long> genreWeights = toWeightMap(genres);
        Map<String, Long> authorWeights = toWeightMap(authors);
        Map<String, Scored> byKey = new LinkedHashMap<>();

        for (BookMatch b : repo.booksByGenres(userId, genreWeights.keySet().stream().toList())) {
            long weight = genreWeights.getOrDefault(lower(b.genre()), 0L);
            merge(byKey, b.title(), b.author(), b.genre(), weight, "Because you read a lot of " + b.genre());
        }
        for (BookMatch b : repo.booksByAuthors(userId, authorWeights.keySet().stream().toList())) {
            long weight = authorWeights.getOrDefault(lower(b.author()), 0L);
            merge(byKey, b.title(), b.author(), b.genre(), weight, "Because you read a lot by " + b.author());
        }

        List<SimilarUser> similar = repo.similarUsers(userId, MIN_SHARED_BOOKS);
        if (!similar.isEmpty()) {
            Map<UUID, Long> sharedByUser = new HashMap<>();
            for (SimilarUser s : similar) sharedByUser.put(s.userId(), s.shared());
            List<UUID> ids = similar.stream().map(SimilarUser::userId).toList();
            for (CollabBook b : repo.booksOwnedBy(userId, ids)) {
                long weight = sharedByUser.getOrDefault(b.ownerId(), 0L);
                merge(byKey, b.title(), b.author(), b.genre(), weight, "Readers with similar books also have this");
            }
        }

        return byKey.values().stream()
            .sorted((a, c) -> Double.compare(c.score(), a.score()))
            .limit(MAX_RESULTS)
            .map(s -> new RecommendationItem(s.title(), s.author(), s.genre(), s.reason()))
            .toList();
    }

    /** Adds a scored contribution for a book; combines scores and keeps the reason with the strongest signal. */
    private void merge(Map<String, Scored> byKey, String title, String author, String genre,
                        long weight, String reason) {
        String key = lower(title) + "|" + lower(author);
        Scored added = new Scored(title, author, genre, reason, weight);
        byKey.merge(key, added, (existing, incoming) -> new Scored(
            existing.title(), existing.author(), existing.genre(),
            incoming.score() > existing.score() ? incoming.reason() : existing.reason(),
            existing.score() + incoming.score()));
    }

    private Map<String, Long> toWeightMap(List<WeightedLabel> labels) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (WeightedLabel l : labels) map.put(l.label(), l.weight());
        return map;
    }

    private static String lower(String s) {
        return s == null ? null : s.toLowerCase(Locale.ROOT);
    }
}
