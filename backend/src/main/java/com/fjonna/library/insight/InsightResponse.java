package com.fjonna.library.insight;

import java.util.List;

public record InsightResponse(InsightStats stats, List<String> sentences) {
}
