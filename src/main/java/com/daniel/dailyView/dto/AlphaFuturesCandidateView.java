package com.daniel.dailyView.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AlphaFuturesCandidateView(
        String alphaId,
        String alphaSymbol,
        String cexCoinName,
        String futuresSymbol,
        String chainId,
        String contractAddress,
        Instant listingTime,
        BigDecimal currentOpenInterestUsdt,
        BigDecimal monthlyPumpPct,
        BigDecimal top10HoldingRatioPct,
        BigDecimal recentWeekAverageOiRatio,
        BigDecimal recentWeekPeakOiRatio,
        BigDecimal topHolderAmount,
        BigDecimal supplyAmount,
        boolean supplyConsistent,
        String supplyDenominatorSource,
        boolean passed,
        List<String> rejectReasons
) {
}
