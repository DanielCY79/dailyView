package com.daniel.dailyView.dto;

import java.math.BigDecimal;

public record AlphaFuturesRulesView(
        BigDecimal minOpenInterestUsdt,
        BigDecimal maxMonthlyPumpPct,
        BigDecimal minTop10HoldingRatioPct,
        BigDecimal minWeeklyAverageOiRatio,
        BigDecimal minWeeklyPeakOiRatio,
        int recentWindowDays
) {
}
