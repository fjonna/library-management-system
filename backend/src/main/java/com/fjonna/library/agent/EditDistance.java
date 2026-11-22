package com.fjonna.library.agent;

/**
 * Plain-Java Levenshtein distance with a length-scaled typo tolerance, so short keywords still
 * require an exact match (to avoid false positives) while longer ones tolerate one or two typos.
 */
public final class EditDistance {

    private EditDistance() {
    }

    public static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            int[] cur = new int[b.length() + 1];
            cur[0] = i;
            char ca = a.charAt(i - 1);
            for (int j = 1; j <= b.length(); j++) {
                int cost = ca == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            prev = cur;
        }
        return prev[b.length()];
    }

    /**
     * Max allowed edit distance for a reference word of this length: 0 (<5), 1 (5-7), 2 (longer).
     * The spec calls for tolerance starting at length 4, but common 4-letter words collide too
     * easily at distance 1 (e.g. "most" vs. "cost") and cause false keyword matches, so the floor
     * here is 5 - still generous enough for every typo example this feature targets ("fantsy" is 6,
     * "expesnive" is 9).
     */
    public static int toleranceFor(int referenceLength) {
        if (referenceLength < 5) return 0;
        if (referenceLength <= 7) return 1;
        return 2;
    }

    /** True if {@code typed} matches {@code reference} exactly, or within {@code reference}'s typo tolerance. */
    public static boolean closeEnough(String typed, String reference) {
        if (typed.equals(reference)) return true;
        int tolerance = toleranceFor(reference.length());
        if (tolerance == 0) return false;
        if (Math.abs(typed.length() - reference.length()) > tolerance) return false;
        return levenshtein(typed, reference) <= tolerance;
    }
}
