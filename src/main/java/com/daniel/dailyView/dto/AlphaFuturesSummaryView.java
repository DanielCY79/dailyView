package com.daniel.dailyView.dto;

public record AlphaFuturesSummaryView(
        int totalAlphaTokens,
        int mappedCandidates,
        int passedCandidates,
        int rejectedCandidates,
        int returnedCandidates
) {
}
