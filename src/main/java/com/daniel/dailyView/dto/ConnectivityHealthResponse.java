package com.daniel.dailyView.dto;

import java.time.Instant;
import java.util.Map;

public record ConnectivityHealthResponse(
        String status,
        Instant timestamp,
        Map<String, ConnectivityHealthNode> components
) {
}
