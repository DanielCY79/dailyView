package com.daniel.dailyView.domain;

import java.math.BigDecimal;

public record HolderConcentrationMetrics(
        BigDecimal topHolderAmount,
        BigDecimal supplyAmount,
        BigDecimal topHoldingRatioPct,
        boolean supplyConsistent,
        String denominatorSource
) {
}
