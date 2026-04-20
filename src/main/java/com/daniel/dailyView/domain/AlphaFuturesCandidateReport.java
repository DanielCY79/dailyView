package com.daniel.dailyView.domain;

import java.math.BigDecimal;
import java.util.List;

import com.daniel.dailyView.dto.AlphaFuturesCandidateView;

public record AlphaFuturesCandidateReport(
        AlphaFuturesUniverseEntry universeEntry,
        BigDecimal currentOpenInterestUsdt,
        List<OpenInterestPoint> openInterestHistory,
        List<KlineCandle> priceKlines,
        List<TopHolder> topHolders,
        PriceExplosionMetrics priceExplosionMetrics,
        OpenInterestAnomalyMetrics openInterestAnomalyMetrics,
        HolderConcentrationMetrics holderConcentrationMetrics,
        boolean supportedChain,
        boolean passed,
        List<String> rejectReasons
) {

    public AlphaFuturesCandidateView toView() {
        return new AlphaFuturesCandidateView(
                universeEntry.alphaToken().alphaId(),
                universeEntry.alphaToken().symbol(),
                universeEntry.alphaToken().cexCoinName(),
                universeEntry.futuresContract().symbol(),
                universeEntry.alphaToken().chainId(),
                universeEntry.alphaToken().contractAddress(),
                universeEntry.alphaToken().listingTime(),
                currentOpenInterestUsdt,
                priceExplosionMetrics.monthlyPumpPct(),
                holderConcentrationMetrics.topHoldingRatioPct(),
                openInterestAnomalyMetrics.recentWeekAverageRatio(),
                openInterestAnomalyMetrics.recentWeekPeakRatio(),
                holderConcentrationMetrics.topHolderAmount(),
                holderConcentrationMetrics.supplyAmount(),
                holderConcentrationMetrics.supplyConsistent(),
                holderConcentrationMetrics.denominatorSource(),
                passed,
                rejectReasons
        );
    }
}
