package com.daniel.dailyView.dto;

import java.util.Map;

public record ConnectivityHealthNode(
        String status,
        Map<String, Object> details,
        Map<String, ConnectivityHealthNode> components
) {
}
