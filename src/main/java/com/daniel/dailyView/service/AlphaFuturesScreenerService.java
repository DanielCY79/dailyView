package com.daniel.dailyView.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.daniel.dailyView.client.BinanceFuturesClient;
import com.daniel.dailyView.client.ChainbaseClient;
import com.daniel.dailyView.config.AlphaFuturesProperties;
import com.daniel.dailyView.domain.AlphaFuturesRules;
import com.daniel.dailyView.domain.AlphaFuturesUniverseEntry;
import com.daniel.dailyView.domain.AlphaToken;
import com.daniel.dailyView.domain.HolderConcentrationMetrics;
import com.daniel.dailyView.domain.OpenInterestAnomalyMetrics;
import com.daniel.dailyView.domain.OpenInterestPoint;
import com.daniel.dailyView.domain.PriceExplosionMetrics;
import com.daniel.dailyView.domain.TopHolder;
import com.daniel.dailyView.dto.AlphaFuturesCandidateView;
import com.daniel.dailyView.dto.AlphaFuturesRulesView;
import com.daniel.dailyView.dto.AlphaFuturesScreenRequest;
import com.daniel.dailyView.dto.AlphaFuturesScreenResponse;
import com.daniel.dailyView.dto.AlphaFuturesSummaryView;

@Service
public class AlphaFuturesScreenerService {

    private final AlphaFuturesProperties properties;
    private final AlphaTokenUniverseService alphaTokenUniverseService;
    private final BinanceFuturesClient binanceFuturesClient;
    private final ChainbaseClient chainbaseClient;
    private final PriceExplosionCalculator priceExplosionCalculator;
    private final OpenInterestAnomalyCalculator openInterestAnomalyCalculator;
    private final HolderConcentrationCalculator holderConcentrationCalculator;

    public AlphaFuturesScreenerService(
            AlphaFuturesProperties properties,
            AlphaTokenUniverseService alphaTokenUniverseService,
            BinanceFuturesClient binanceFuturesClient,
            ChainbaseClient chainbaseClient,
            PriceExplosionCalculator priceExplosionCalculator,
            OpenInterestAnomalyCalculator openInterestAnomalyCalculator,
            HolderConcentrationCalculator holderConcentrationCalculator) {
        this.properties = properties;
        this.alphaTokenUniverseService = alphaTokenUniverseService;
        this.binanceFuturesClient = binanceFuturesClient;
        this.chainbaseClient = chainbaseClient;
        this.priceExplosionCalculator = priceExplosionCalculator;
        this.openInterestAnomalyCalculator = openInterestAnomalyCalculator;
        this.holderConcentrationCalculator = holderConcentrationCalculator;
    }

    public AlphaFuturesScreenResponse screen(AlphaFuturesScreenRequest request) {
        AlphaFuturesRules rules = resolveRules(request);
        List<AlphaToken> alphaTokens = alphaTokenUniverseService.fetchAlphaTokens();
        List<AlphaFuturesUniverseEntry> mappedUniverse = alphaTokenUniverseService.loadMappedUniverse(alphaTokens);

        List<AlphaFuturesCandidateView> evaluatedCandidates = mappedUniverse.stream()
                .map(candidate -> evaluateCandidate(candidate, rules))
                .sorted(Comparator.comparing(
                                AlphaFuturesCandidateView::currentOpenInterestUsdt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AlphaFuturesCandidateView::cexCoinName, Comparator.nullsLast(String::compareTo)))
                .toList();

        int passedCount = (int) evaluatedCandidates.stream().filter(AlphaFuturesCandidateView::passed).count();
        int rejectedCount = evaluatedCandidates.size() - passedCount;

        List<AlphaFuturesCandidateView> returnedCandidates = request.includeRejected()
                ? evaluatedCandidates
                : evaluatedCandidates.stream().filter(AlphaFuturesCandidateView::passed).toList();

        AlphaFuturesSummaryView summary = new AlphaFuturesSummaryView(
                alphaTokens.size(),
                mappedUniverse.size(),
                passedCount,
                rejectedCount,
                returnedCandidates.size()
        );

        AlphaFuturesRulesView rulesView = new AlphaFuturesRulesView(
                rules.minOpenInterestUsdt(),
                rules.maxMonthlyPumpPct(),
                rules.minTop10HoldingRatioPct(),
                rules.minWeeklyAverageOiRatio(),
                rules.minWeeklyPeakOiRatio(),
                rules.recentWindowDays()
        );

        return new AlphaFuturesScreenResponse(
                Instant.now(),
                rulesView,
                summary,
                returnedCandidates
        );
    }

    private AlphaFuturesCandidateView evaluateCandidate(AlphaFuturesUniverseEntry candidate, AlphaFuturesRules rules) {
        List<String> rejectReasons = new ArrayList<>();
        BigDecimal currentOpenInterestUsdt = null;
        BigDecimal monthlyPumpPct = null;
        BigDecimal top10HoldingRatioPct = null;
        BigDecimal recentWeekAverageOiRatio = null;
        BigDecimal recentWeekPeakOiRatio = null;
        BigDecimal topHolderAmount = null;
        BigDecimal supplyAmount = null;
        boolean supplyConsistent = false;
        String supplyDenominatorSource = "binanceTotalSupply";

        String futuresSymbol = candidate.futuresContract().symbol();

        try {
            currentOpenInterestUsdt = scale(
                    binanceFuturesClient.fetchLatestOpenInterestValueUsdt(
                            futuresSymbol,
                            properties.getCurrentOiPeriod()));
            if (currentOpenInterestUsdt.compareTo(rules.minOpenInterestUsdt()) < 0) {
                rejectReasons.add("open_interest_below_threshold");
            }
        } catch (RuntimeException ex) {
            rejectReasons.add("open_interest_unavailable");
        }

        try {
            PriceExplosionMetrics metrics = priceExplosionCalculator.calculate(
                    binanceFuturesClient.fetchDailyKlines(
                            futuresSymbol,
                            properties.getPriceKlineInterval(),
                            properties.getMonthlyKlineLimit()));
            monthlyPumpPct = metrics.monthlyPumpPct();
            if (monthlyPumpPct == null) {
                rejectReasons.add("monthly_price_data_unavailable");
            } else if (monthlyPumpPct.compareTo(rules.maxMonthlyPumpPct()) >= 0) {
                rejectReasons.add("monthly_price_exploded");
            }
        } catch (RuntimeException ex) {
            rejectReasons.add("monthly_price_data_unavailable");
        }

        try {
            List<OpenInterestPoint> history = binanceFuturesClient.fetchOpenInterestHistory(
                    futuresSymbol,
                    properties.getOiHistoryPeriod(),
                    properties.getOiHistoryLimit());
            OpenInterestAnomalyMetrics anomalyMetrics = openInterestAnomalyCalculator.calculate(
                    history,
                    rules.recentWindowDays());

            recentWeekAverageOiRatio = anomalyMetrics.recentWeekAverageRatio();
            recentWeekPeakOiRatio = anomalyMetrics.recentWeekPeakRatio();

            boolean meetsAverageThreshold = recentWeekAverageOiRatio != null
                    && recentWeekAverageOiRatio.compareTo(rules.minWeeklyAverageOiRatio()) >= 0;
            boolean meetsPeakThreshold = recentWeekPeakOiRatio != null
                    && recentWeekPeakOiRatio.compareTo(rules.minWeeklyPeakOiRatio()) >= 0;

            if (!meetsAverageThreshold && !meetsPeakThreshold) {
                rejectReasons.add("weekly_open_interest_not_anomalous");
            }
        } catch (RuntimeException ex) {
            rejectReasons.add("weekly_open_interest_unavailable");
        }

        try {
            if (!chainbaseClient.supportsChain(candidate.alphaToken().chainId())) {
                rejectReasons.add("unsupported_chain_for_holder_scan");
            } else {
                List<TopHolder> topHolders = chainbaseClient.fetchTopHolders(
                        candidate.alphaToken().chainId(),
                        candidate.alphaToken().contractAddress(),
                        properties.getTopHolderLimit());

                HolderConcentrationMetrics concentrationMetrics = holderConcentrationCalculator.calculate(
                        topHolders,
                        candidate.alphaToken().totalSupply());

                top10HoldingRatioPct = concentrationMetrics.topHoldingRatioPct();
                topHolderAmount = scale(concentrationMetrics.topHolderAmount());
                supplyAmount = scale(concentrationMetrics.supplyAmount());
                supplyConsistent = concentrationMetrics.supplyConsistent();
                supplyDenominatorSource = concentrationMetrics.denominatorSource();

                if (top10HoldingRatioPct == null) {
                    rejectReasons.add("holder_concentration_unavailable");
                } else if (!supplyConsistent) {
                    rejectReasons.add("holder_supply_inconsistent");
                } else if (top10HoldingRatioPct.compareTo(rules.minTop10HoldingRatioPct()) < 0) {
                    rejectReasons.add("holder_concentration_below_threshold");
                }
            }
        } catch (RuntimeException ex) {
            rejectReasons.add("holder_concentration_unavailable");
        }

        return new AlphaFuturesCandidateView(
                candidate.alphaToken().alphaId(),
                candidate.alphaToken().symbol(),
                candidate.alphaToken().cexCoinName(),
                futuresSymbol,
                candidate.alphaToken().chainId(),
                candidate.alphaToken().contractAddress(),
                candidate.alphaToken().listingTime(),
                currentOpenInterestUsdt,
                monthlyPumpPct,
                top10HoldingRatioPct,
                recentWeekAverageOiRatio,
                recentWeekPeakOiRatio,
                topHolderAmount,
                supplyAmount,
                supplyConsistent,
                supplyDenominatorSource,
                rejectReasons.isEmpty(),
                List.copyOf(rejectReasons)
        );
    }

    private AlphaFuturesRules resolveRules(AlphaFuturesScreenRequest request) {
        return new AlphaFuturesRules(
                request.minOpenInterestUsdt() != null ? request.minOpenInterestUsdt() : properties.getMinOpenInterestUsdt(),
                request.maxMonthlyPumpPct() != null ? request.maxMonthlyPumpPct() : properties.getMaxMonthlyPumpPct(),
                request.minTop10HoldingRatioPct() != null
                        ? request.minTop10HoldingRatioPct()
                        : properties.getMinTop10HoldingRatioPct(),
                request.minWeeklyAverageOiRatio() != null
                        ? request.minWeeklyAverageOiRatio()
                        : properties.getMinWeeklyAverageOiRatio(),
                request.minWeeklyPeakOiRatio() != null
                        ? request.minWeeklyPeakOiRatio()
                        : properties.getMinWeeklyPeakOiRatio(),
                properties.getRecentWindowDays()
        );
    }

    private BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }
}
