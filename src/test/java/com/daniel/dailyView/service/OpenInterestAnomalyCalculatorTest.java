package com.daniel.dailyView.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.daniel.dailyView.domain.OpenInterestAnomalyMetrics;
import com.daniel.dailyView.domain.OpenInterestPoint;

class OpenInterestAnomalyCalculatorTest {

    private final OpenInterestAnomalyCalculator calculator = new OpenInterestAnomalyCalculator();

    @Test
    void shouldCompareRecentWeekAgainstPreviousWindow() {
        List<OpenInterestPoint> history = List.of(
                point("10", 1),
                point("10", 2),
                point("12", 3),
                point("11", 4),
                point("20", 5),
                point("21", 6),
                point("24", 7)
        );

        OpenInterestAnomalyMetrics metrics = calculator.calculate(history, 3);

        assertEquals(new BigDecimal("2.0155"), metrics.recentWeekAverageRatio());
        assertEquals(new BigDecimal("2.2857"), metrics.recentWeekPeakRatio());
    }

    private OpenInterestPoint point(String value, long dayOffset) {
        return new OpenInterestPoint(new BigDecimal(value), Instant.ofEpochSecond(dayOffset * 86_400));
    }
}
