package com.daniel.dailyView.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.daniel.dailyView.domain.KlineCandle;
import com.daniel.dailyView.domain.PriceExplosionMetrics;

class PriceExplosionCalculatorTest {

    private final PriceExplosionCalculator calculator = new PriceExplosionCalculator();

    @Test
    void shouldCalculateMonthlyPumpPctFromHighestHighAndLowestLow() {
        List<KlineCandle> candles = List.of(
                new KlineCandle(new BigDecimal("2.20"), new BigDecimal("1.00")),
                new KlineCandle(new BigDecimal("3.60"), new BigDecimal("1.40")),
                new KlineCandle(new BigDecimal("4.50"), new BigDecimal("1.80"))
        );

        PriceExplosionMetrics metrics = calculator.calculate(candles);

        assertEquals(new BigDecimal("350.00"), metrics.monthlyPumpPct());
    }
}
