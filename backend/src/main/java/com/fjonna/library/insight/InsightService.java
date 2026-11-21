package com.fjonna.library.insight;

import com.fjonna.library.insight.InsightRepository.Averages;
import com.fjonna.library.insight.InsightRepository.BookPrice;
import com.fjonna.library.insight.InsightRepository.Counts;
import com.fjonna.library.insight.InsightRepository.GenreCount;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InsightService {

    private static final int SHORT_PAGES = 250;
    private static final int LONG_PAGES = 450;

    private final InsightRepository repo;

    @Transactional(readOnly = true)
    public InsightResponse insights(UUID userId) {
        Counts counts = repo.counts(userId);
        if (counts.total() == 0) {
            InsightStats stats = new InsightStats(0, 0, 0, 0, null, null, null);
            return new InsightResponse(stats, List.of("You don't have any books yet."));
        }

        Averages averages = repo.averages(userId);
        List<GenreCount> genres = repo.genreCounts(userId);
        BookPrice mostExpensive = repo.mostExpensiveBook(userId).orElse(null);
        String topGenre = genres.isEmpty() ? null : genres.get(0).genre();

        InsightStats stats = new InsightStats(counts.total(), counts.finished(), counts.reading(), counts.toRead(),
            averages.avgPrice(), averages.avgPages(), topGenre);
        return new InsightResponse(stats, sentences(counts, genres, averages.avgPages(), mostExpensive));
    }

    @Transactional(readOnly = true)
    public String summary(UUID userId) {
        InsightResponse insights = insights(userId);
        InsightStats s = insights.stats();
        if (s.totalBooks() == 0) {
            return "You don't have any books in your library yet. Add a few to get personalized insights.";
        }

        StringBuilder sb = new StringBuilder("You own ").append(s.totalBooks()).append(" book")
            .append(book(s.totalBooks())).append(", with ").append(s.finished()).append(" finished, ")
            .append(s.reading()).append(" currently reading, and ").append(s.toRead()).append(" still to read.");
        for (String sentence : insights.sentences()) {
            if (sentence.startsWith("You have finished")) continue;
            sb.append(' ').append(sentence);
        }
        return sb.toString();
    }

    private List<String> sentences(Counts counts, List<GenreCount> genres, Double avgPages, BookPrice mostExpensive) {
        List<String> sentences = new ArrayList<>();
        String genreSentence = genreSentence(genres);
        if (genreSentence != null) sentences.add(genreSentence);
        String lengthSentence = lengthSentence(avgPages);
        if (lengthSentence != null) sentences.add(lengthSentence);
        sentences.add("You have finished " + counts.finished() + " of " + counts.total() + " book" + book(counts.total()) + ".");
        if (mostExpensive != null) {
            sentences.add("Your most expensive book is '" + mostExpensive.title() + "' at " + mostExpensive.price() + ".");
        }
        return sentences;
    }

    private String genreSentence(List<GenreCount> genres) {
        if (genres.isEmpty()) return null;
        long max = genres.get(0).count();
        List<String> tied = genres.stream().filter(g -> g.count() == max).map(GenreCount::genre).toList();
        if (tied.size() == 1) {
            return tied.get(0) + " is your most read genre (" + max + " book" + book(max) + ").";
        }
        return joinWithAnd(tied) + " are tied as your most read genres (" + max + " books each).";
    }

    private String lengthSentence(Double avgPages) {
        if (avgPages == null) return null;
        long rounded = Math.round(avgPages);
        if (rounded < SHORT_PAGES) return "You tend to read short books (average " + rounded + " pages).";
        if (rounded > LONG_PAGES) return "You tend to read long books (average " + rounded + " pages).";
        return null;
    }

    private String joinWithAnd(List<String> items) {
        if (items.size() == 1) return items.get(0);
        if (items.size() == 2) return items.get(0) + " and " + items.get(1);
        return String.join(", ", items.subList(0, items.size() - 1)) + ", and " + items.get(items.size() - 1);
    }

    private String book(long count) {
        return count == 1 ? "" : "s";
    }
}
