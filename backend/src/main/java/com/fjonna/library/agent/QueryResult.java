package com.fjonna.library.agent;

import java.util.List;

/** Shape the frontend renders: a title, a summary sentence and a table. */
public record QueryResult(
    String intent,
    String title,
    String summary,
    List<String> columns,
    List<List<Object>> rows) {
}
