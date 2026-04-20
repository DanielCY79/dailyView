package com.daniel.dailyView.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record OpenInterestPoint(
        BigDecimal openInterestValueUsdt,
        Instant timestamp
) {
}
