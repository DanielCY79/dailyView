package com.daniel.dailyView.domain;

import java.math.BigDecimal;

public record AlphaFuturesRules(
        BigDecimal minOpenInterestUsdt,
        BigDecimal maxMonthlyPumpPct,
        BigDecimal minTop10HoldingRatioPct,
        BigDecimal minWeeklyAverageOiRatio,
        BigDecimal minWeeklyPeakOiRatio,
        int recentWindowDays
) {
}
