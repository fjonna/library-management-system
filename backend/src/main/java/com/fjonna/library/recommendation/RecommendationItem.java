package com.fjonna.library.recommendation;

/** A single suggested book, with a short human-readable reason it was picked. */
public record RecommendationItem(String title, String author, String genre, String reason) {
}
