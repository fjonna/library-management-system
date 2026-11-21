package com.fjonna.library.insight;

import java.math.BigDecimal;

/** Numbers for the stat tiles. Null fields mean there is no data to compute them from. */
public record InsightStats(
    long totalBooks,
    long finished,
    long reading,
    long toRead,
    BigDecimal averagePrice,
    Double averagePages,
    String topGenre) {
}
