package com.fjonna.library.agent;

/** A book column a question can ask about. {@link SqlBuilder} maps each value to a fixed, whitelisted SQL column. */
public enum Field {
    PRICE, PAGES, TITLE, AUTHOR, GENRE, STATUS, CREATED_AT
}
