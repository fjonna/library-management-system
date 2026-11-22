package com.fjonna.library.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Lower-cases text and folds Albanian diacritics so "shtrenjtë" and "shtrenjte" compare equal. */
public final class Normalizer {

    private Normalizer() {
    }

    public static String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase(Locale.ROOT).replace('ë', 'e').replace('ç', 'c');
    }

    /** Splits already-normalized text into letter/digit tokens, dropping surrounding punctuation. */
    public static List<String> words(String normalized) {
        List<String> words = new ArrayList<>();
        for (String w : normalized.split("[^\\p{L}\\p{Nd}]+")) {
            if (!w.isBlank()) words.add(w);
        }
        return words;
    }

    /** Strips leading/trailing punctuation from a single token (e.g. "faqe?" -> "faqe"). */
    public static String stripPunctuation(String token) {
        return token.replaceAll("^[^\\p{L}\\p{Nd}]+", "").replaceAll("[^\\p{L}\\p{Nd}]+$", "");
    }
}
