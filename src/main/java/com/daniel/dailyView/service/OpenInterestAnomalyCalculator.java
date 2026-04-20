package com.daniel.dailyView.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.daniel.dailyView.domain.OpenInterestAnomalyMetrics;
import com.daniel.dailyView.domain.OpenInterestPoint;

@Component
public class OpenInterestAnomalyCalculator {

    public OpenInterestAnomalyMetrics calculate(List<OpenInterestPoint> history, int recentWindowDays) {
        if (history == null || history.size() <= recentWindowDays || recentWindowDays <= 0) {
            return new OpenInterestAnomalyMetrics(null, null);
        }

        List<OpenInterestPoint> sorted = history.stream()
                .sorted(Comparator.comparing(OpenInterestPoint::timestamp))
                .toList();

        List<BigDecimal> previous = sorted.subList(0, sorted.size() - recentWindowDays).stream()
                .map(OpenInterestPoint::openInterestValueUsdt)
                .toList();

        List<BigDecimal> recent = sorted.subList(sorted.size() - recentWindowDays, sorted.size()).stream()
                .map(OpenInterestPoint::openInterestValueUsdt)
                .toList();

        if (previous.isEmpty() || recent.isEmpty()) {
            return new OpenInterestAnomalyMetrics(null, null);
        }

        BigDecimal previousAverage = average(previous);
        BigDecimal recentAverage = average(recent);
        BigDecimal previousMedian = median(previous);
        BigDecimal recentPeak = recent.stream().max(Comparator.naturalOrder()).orElse(null);

        return new OpenInterestAnomalyMetrics(
                divide(recentAverage, previousAverage),
                divide(recentPeak, previousMedian)
        );
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 8, RoundingMode.HALF_UP);
    }

    private BigDecimal median(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return null;
        }
        List<BigDecimal> sorted = values.stream().sorted().toList();
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(middle);
        }
        return sorted.get(middle - 1)
                .add(sorted.get(middle))
                .divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_UP);
    }

    private BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.signum() == 0) {
            return null;
        }
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP);
    }
}
