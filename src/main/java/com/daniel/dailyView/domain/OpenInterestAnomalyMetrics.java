package com.daniel.dailyView.domain;

import java.math.BigDecimal;

public record OpenInterestAnomalyMetrics(
        BigDecimal recentWeekAverageRatio,
        BigDecimal recentWeekPeakRatio
) {
}
