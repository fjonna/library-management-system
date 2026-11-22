package com.fjonna.library.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class PagingTest {

    @Test
    void defaultsToPageZeroAndSize50WhenBothAbsent() {
        Pageable p = Paging.of(null, null);
        assertThat(p.getPageNumber()).isZero();
        assertThat(p.getPageSize()).isEqualTo(50);
    }

    @Test
    void sizeAboveMaxIsClampedTo100() {
        Pageable p = Paging.of(null, 1000);
        assertThat(p.getPageSize()).isEqualTo(100);
    }

    @Test
    void sizeBelowOneIsClampedToOne() {
        Pageable p = Paging.of(null, 0);
        assertThat(p.getPageSize()).isEqualTo(1);

        Pageable negative = Paging.of(null, -5);
        assertThat(negative.getPageSize()).isEqualTo(1);
    }

    @Test
    void negativePageIsClampedToZero() {
        Pageable p = Paging.of(-3, 10);
        assertThat(p.getPageNumber()).isZero();
    }

    @Test
    void explicitValuesWithinBoundsArePreserved() {
        Pageable p = Paging.of(2, 25);
        assertThat(p.getPageNumber()).isEqualTo(2);
        assertThat(p.getPageSize()).isEqualTo(25);
    }
}
