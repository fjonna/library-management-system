package com.fjonna.library.agent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EditDistanceTest {

    @Test
    void levenshteinOfIdenticalStringsIsZero() {
        assertThat(EditDistance.levenshtein("fantasy", "fantasy")).isZero();
    }

    @Test
    void levenshteinCountsSingleSubstitution() {
        assertThat(EditDistance.levenshtein("fantasy", "fantsay")).isEqualTo(2);
        assertThat(EditDistance.levenshtein("book", "boot")).isEqualTo(1);
    }

    @Test
    void shortWordsRequireExactMatch() {
        assertThat(EditDistance.closeEnough("sa", "sa")).isTrue();
        assertThat(EditDistance.closeEnough("so", "sa")).isFalse();
        assertThat(EditDistance.closeEnough("most", "cost")).isFalse();
    }

    @Test
    void mediumWordsToleratesOneTypo() {
        assertThat(EditDistance.closeEnough("fantsy", "fantasy")).isTrue();
        assertThat(EditDistance.closeEnough("cheep", "cheap")).isTrue();
        assertThat(EditDistance.closeEnough("chexx", "cheap")).isFalse();
    }

    @Test
    void longWordsTolerateTwoTypos() {
        assertThat(EditDistance.closeEnough("expesnive", "expensive")).isTrue();
        assertThat(EditDistance.closeEnough("expsnsive", "expensive")).isTrue();
    }

    @Test
    void toleranceScalesWithReferenceLength() {
        assertThat(EditDistance.toleranceFor(3)).isZero();
        assertThat(EditDistance.toleranceFor(5)).isEqualTo(1);
        assertThat(EditDistance.toleranceFor(7)).isEqualTo(1);
        assertThat(EditDistance.toleranceFor(8)).isEqualTo(2);
    }
}
