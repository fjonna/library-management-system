package com.fjonna.library.agent;

/** What to compute over the matched books. GROUP_COUNT is COUNT(*) per group (vs. plain COUNT which has no groupBy). */
public enum Aggregate {
    COUNT, SUM, AVG, MIN, MAX, LIST, GROUP_COUNT
}
