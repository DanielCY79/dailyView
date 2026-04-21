package com.daniel.dailyView.dto;

import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;

public record AlphaFuturesRunPayloadView(
        String sourceName,
        String endpointName,
        String entityKey,
        boolean success,
        String requestUri,
        JsonNode requestParams,
        JsonNode responseBody,
        String errorMessage,
        Instant collectedAt
) {
}
