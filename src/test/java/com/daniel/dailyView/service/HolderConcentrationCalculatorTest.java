package com.daniel.dailyView.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.daniel.dailyView.domain.HolderConcentrationMetrics;
import com.daniel.dailyView.domain.TopHolder;

class HolderConcentrationCalculatorTest {

    private final HolderConcentrationCalculator calculator = new HolderConcentrationCalculator();

    @Test
    void shouldCalculateTopHolderRatioAgainstTotalSupply() {
        List<TopHolder> topHolders = List.of(
                new TopHolder("a", new BigDecimal("40")),
                new TopHolder("b", new BigDecimal("30")),
                new TopHolder("c", new BigDecimal("25"))
        );

        HolderConcentrationMetrics metrics = calculator.calculate(topHolders, new BigDecimal("100"));

        assertEquals(new BigDecimal("95.00"), metrics.topHoldingRatioPct());
        assertTrue(metrics.supplyConsistent());
    }
}
