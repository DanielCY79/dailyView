package com.daniel.dailyView.dto;

import java.time.Instant;
import java.util.List;

public record AlphaFuturesScreenResponse(
        Instant generatedAt,
        AlphaFuturesRulesView rules,
        AlphaFuturesSummaryView summary,
        List<AlphaFuturesCandidateView> candidates
) {
}
