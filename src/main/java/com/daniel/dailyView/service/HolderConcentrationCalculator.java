package com.daniel.dailyView.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Component;

import com.daniel.dailyView.domain.HolderConcentrationMetrics;
import com.daniel.dailyView.domain.TopHolder;

@Component
public class HolderConcentrationCalculator {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    public HolderConcentrationMetrics calculate(List<TopHolder> topHolders, BigDecimal totalSupply) {
        BigDecimal topHolderAmount = topHolders == null
                ? BigDecimal.ZERO
                : topHolders.stream()
                        .map(TopHolder::amount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalSupply == null || totalSupply.signum() <= 0) {
            return new HolderConcentrationMetrics(topHolderAmount, null, null, false, "binanceTotalSupply");
        }

        BigDecimal ratio = topHolderAmount
                .divide(totalSupply, 8, RoundingMode.HALF_UP)
                .multiply(ONE_HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);

        boolean supplyConsistent = ratio.compareTo(new BigDecimal("100")) <= 0;

        return new HolderConcentrationMetrics(
                topHolderAmount,
                totalSupply,
                ratio,
                supplyConsistent,
                "binanceTotalSupply"
        );
    }
}
