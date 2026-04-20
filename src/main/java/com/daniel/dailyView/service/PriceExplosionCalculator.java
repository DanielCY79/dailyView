package com.daniel.dailyView.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.daniel.dailyView.domain.KlineCandle;
import com.daniel.dailyView.domain.PriceExplosionMetrics;

@Component
public class PriceExplosionCalculator {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public PriceExplosionMetrics calculate(List<KlineCandle> candles) {
        if (candles == null || candles.isEmpty()) {
            return new PriceExplosionMetrics(null, null, null);
        }

        BigDecimal maxHighPrice = candles.stream()
                .map(KlineCandle::highPrice)
                .filter(value -> value != null && value.signum() > 0)
                .max(Comparator.naturalOrder())
                .orElse(null);

        BigDecimal minLowPrice = candles.stream()
                .map(KlineCandle::lowPrice)
                .filter(value -> value != null && value.signum() > 0)
                .min(Comparator.naturalOrder())
                .orElse(null);

        if (maxHighPrice == null || minLowPrice == null) {
            return new PriceExplosionMetrics(maxHighPrice, minLowPrice, null);
        }

        BigDecimal monthlyPumpPct = maxHighPrice
                .divide(minLowPrice, 8, RoundingMode.HALF_UP)
                .subtract(BigDecimal.ONE)
                .multiply(ONE_HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);

        return new PriceExplosionMetrics(maxHighPrice, minLowPrice, monthlyPumpPct);
    }
}
