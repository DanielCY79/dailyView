package com.daniel.dailyView.domain;

import java.math.BigDecimal;

public record PriceExplosionMetrics(
        BigDecimal maxHighPrice,
        BigDecimal minLowPrice,
        BigDecimal monthlyPumpPct
) {
}
