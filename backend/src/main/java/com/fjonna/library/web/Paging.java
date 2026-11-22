package com.fjonna.library.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/** Optional page/size query params for list endpoints: size defaults to 50, capped at 100. */
final class Paging {

    static final int DEFAULT_SIZE = 50;
    static final int MAX_SIZE = 100;

    private Paging() {
    }

    static Pageable of(Integer page, Integer size) {
        int p = page == null ? 0 : Math.max(page, 0);
        int s = size == null ? DEFAULT_SIZE : Math.min(Math.max(size, 1), MAX_SIZE);
        return PageRequest.of(p, s);
    }
}
