package com.fjonna.library.agent;

import java.util.List;

/**
 * Shape the frontend renders: a title, a summary sentence and a table.
 * suggestedQuestions is true when columns/rows actually hold example
 * questions to show as clickable chips (UNKNOWN intent, or a user asking an
 * admin-only question) rather than a real result table.
 */
public record QueryResult(
    String intent,
    String understoodAs,
    String title,
    String summary,
    List<String> columns,
    List<List<Object>> rows,
    boolean suggestedQuestions) {
}
