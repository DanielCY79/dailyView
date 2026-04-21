package com.daniel.dailyView.dto;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;

public record AlphaFuturesRunView(
        long runId,
        String jobName,
        String triggerType,
        String status,
        Instant startedAt,
        Instant endedAt,
        Integer durationMs,
        JsonNode rules,
        Integer totalAlphaTokens,
        Integer mappedCandidates,
        Integer passedCandidates,
        Integer rejectedCandidates,
        String errorMessage
) {
}
