package com.daniel.dailyView.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.daniel.dailyView.client.BinanceFuturesClient;
import com.daniel.dailyView.client.TopHolderClient;
import com.daniel.dailyView.client.TopHolderRequestSpec;
import com.daniel.dailyView.config.AlphaFuturesProperties;
import com.daniel.dailyView.domain.AlphaFuturesCandidateReport;
import com.daniel.dailyView.domain.AlphaFuturesRules;
import com.daniel.dailyView.domain.AlphaFuturesScreeningReport;
import com.daniel.dailyView.domain.AlphaFuturesUniverseEntry;
import com.daniel.dailyView.domain.AlphaToken;
import com.daniel.dailyView.domain.FuturesContract;
import com.daniel.dailyView.domain.HolderConcentrationMetrics;
import com.daniel.dailyView.domain.KlineCandle;
import com.daniel.dailyView.domain.OpenInterestAnomalyMetrics;
import com.daniel.dailyView.domain.OpenInterestPoint;
import com.daniel.dailyView.domain.PriceExplosionMetrics;
import com.daniel.dailyView.domain.TopHolder;
import com.daniel.dailyView.dto.AlphaFuturesScreenRequest;
import com.daniel.dailyView.dto.AlphaFuturesScreenResponse;

@Service
public class AlphaFuturesScreenerService {

    private static final Logger log = LoggerFactory.getLogger(AlphaFuturesScreenerService.class);

    private final AlphaFuturesProperties properties;
    private final AlphaTokenUniverseService alphaTokenUniverseService;
    private final BinanceFuturesClient binanceFuturesClient;
    private final TopHolderClient topHolderClient;
    private final PriceExplosionCalculator priceExplosionCalculator;
    private final OpenInterestAnomalyCalculator openInterestAnomalyCalculator;
    private final HolderConcentrationCalculator holderConcentrationCalculator;

    public AlphaFuturesScreenerService(
            AlphaFuturesProperties properties,
            AlphaTokenUniverseService alphaTokenUniverseService,
            BinanceFuturesClient binanceFuturesClient,
            TopHolderClient topHolderClient,
            PriceExplosionCalculator priceExplosionCalculator,
            OpenInterestAnomalyCalculator openInterestAnomalyCalculator,
            HolderConcentrationCalculator holderConcentrationCalculator) {
        this.properties = properties;
        this.alphaTokenUniverseService = alphaTokenUniverseService;
        this.binanceFuturesClient = binanceFuturesClient;
        this.topHolderClient = topHolderClient;
        this.priceExplosionCalculator = priceExplosionCalculator;
        this.openInterestAnomalyCalculator = openInterestAnomalyCalculator;
        this.holderConcentrationCalculator = holderConcentrationCalculator;
    }

    public AlphaFuturesScreenResponse screen(AlphaFuturesScreenRequest request) {
        return buildReport(request).toResponse(request.includeRejected());
    }

    public AlphaFuturesScreeningReport buildReport(AlphaFuturesScreenRequest request) {
        AlphaFuturesRules rules = resolveRules(request);
        List<AlphaToken> alphaTokens = alphaTokenUniverseService.fetchAlphaTokens();
        Map<String, FuturesContract> futuresContractsByBaseAsset =
                binanceFuturesClient.fetchUsdtPerpetualContractsByBaseAsset();
        List<AlphaFuturesUniverseEntry> mappedUniverse = alphaTokenUniverseService.loadMappedUniverse(
                alphaTokens,
                futuresContractsByBaseAsset);

        List<AlphaFuturesCandidateReport> candidateReports = mappedUniverse.stream()
                .map(candidate -> evaluateCandidate(candidate, rules))
                .toList();

        return new AlphaFuturesScreeningReport(
                Instant.now(),
                rules,
                alphaTokens,
                futuresContractsByBaseAsset.values().stream().toList(),
                candidateReports
        );
    }

    private AlphaFuturesCandidateReport evaluateCandidate(AlphaFuturesUniverseEntry candidate, AlphaFuturesRules rules) {
        List<String> rejectReasons = new ArrayList<>();
        BigDecimal currentOpenInterestUsdt = null;
        List<OpenInterestPoint> openInterestHistory = List.of();
        List<KlineCandle> priceKlines = List.of();
        List<TopHolder> topHolders = List.of();

        PriceExplosionMetrics priceExplosionMetrics = new PriceExplosionMetrics(null, null, null);
        OpenInterestAnomalyMetrics openInterestAnomalyMetrics = new OpenInterestAnomalyMetrics(null, null);
        HolderConcentrationMetrics holderConcentrationMetrics =
                new HolderConcentrationMetrics(null, null, null, false, "binanceTotalSupply");
        String holderScanErrorMessage = null;

        String futuresSymbol = candidate.futuresContract().symbol();
        TopHolderRequestSpec holderRequestSpec = topHolderClient.describeRequest(
                candidate.alphaToken().chainId(),
                candidate.alphaToken().contractAddress(),
                properties.getTopHolderLimit());
        boolean supportedChain = holderRequestSpec.supportedChain();

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
            priceKlines = binanceFuturesClient.fetchDailyKlines(
                    futuresSymbol,
                    properties.getPriceKlineInterval(),
                    properties.getMonthlyKlineLimit());

            priceExplosionMetrics = priceExplosionCalculator.calculate(priceKlines);
            if (priceExplosionMetrics.monthlyPumpPct() == null) {
                rejectReasons.add("monthly_price_data_unavailable");
            } else if (priceExplosionMetrics.monthlyPumpPct().compareTo(rules.maxMonthlyPumpPct()) >= 0) {
                rejectReasons.add("monthly_price_exploded");
            }
        } catch (RuntimeException ex) {
            rejectReasons.add("monthly_price_data_unavailable");
        }

        try {
            openInterestHistory = binanceFuturesClient.fetchOpenInterestHistory(
                    futuresSymbol,
                    properties.getOiHistoryPeriod(),
                    properties.getOiHistoryLimit());
            openInterestAnomalyMetrics = openInterestAnomalyCalculator.calculate(
                    openInterestHistory,
                    rules.recentWindowDays());

            boolean meetsAverageThreshold = openInterestAnomalyMetrics.recentWeekAverageRatio() != null
                    && openInterestAnomalyMetrics.recentWeekAverageRatio().compareTo(rules.minWeeklyAverageOiRatio()) >= 0;
            boolean meetsPeakThreshold = openInterestAnomalyMetrics.recentWeekPeakRatio() != null
                    && openInterestAnomalyMetrics.recentWeekPeakRatio().compareTo(rules.minWeeklyPeakOiRatio()) >= 0;

            if (!meetsAverageThreshold && !meetsPeakThreshold) {
                rejectReasons.add("weekly_open_interest_not_anomalous");
            }
        } catch (RuntimeException ex) {
            rejectReasons.add("weekly_open_interest_unavailable");
        }

        try {
            if (!supportedChain) {
                rejectReasons.add("unsupported_chain_for_holder_scan");
            } else {
                topHolders = topHolderClient.fetchTopHolders(
                        candidate.alphaToken().chainId(),
                        candidate.alphaToken().contractAddress(),
                        properties.getTopHolderLimit());

                holderConcentrationMetrics = holderConcentrationCalculator.calculate(
                        topHolders,
                        candidate.alphaToken().totalSupply());

                if (holderConcentrationMetrics.topHoldingRatioPct() == null) {
                    rejectReasons.add("holder_concentration_unavailable");
                } else if (!holderConcentrationMetrics.supplyConsistent()) {
                    rejectReasons.add("holder_supply_inconsistent");
                } else if (holderConcentrationMetrics.topHoldingRatioPct()
                        .compareTo(rules.minTop10HoldingRatioPct()) < 0) {
                    rejectReasons.add("holder_concentration_below_threshold");
                }
            }
        } catch (RuntimeException ex) {
            holderScanErrorMessage = ex.getMessage();
            log.warn(
                    "Holder scan failed. chainId={}, contractAddress={}, source={}, endpoint={}, error={}",
                    candidate.alphaToken().chainId(),
                    candidate.alphaToken().contractAddress(),
                    holderRequestSpec.sourceName(),
                    holderRequestSpec.endpointName(),
                    ex.getMessage());
            rejectReasons.add("holder_concentration_unavailable");
        }

        return new AlphaFuturesCandidateReport(
                candidate,
                currentOpenInterestUsdt,
                openInterestHistory,
                priceKlines,
                topHolders,
                priceExplosionMetrics,
                openInterestAnomalyMetrics,
                scaleHolderMetrics(holderConcentrationMetrics),
                holderRequestSpec.sourceName(),
                holderRequestSpec.endpointName(),
                holderRequestSpec.requestUri(),
                holderRequestSpec.requestParams(),
                holderScanErrorMessage,
                supportedChain,
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

    private HolderConcentrationMetrics scaleHolderMetrics(HolderConcentrationMetrics metrics) {
        return new HolderConcentrationMetrics(
                scale(metrics.topHolderAmount()),
                scale(metrics.supplyAmount()),
                metrics.topHoldingRatioPct(),
                metrics.supplyConsistent(),
                metrics.denominatorSource()
        );
    }

    private BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(4, RoundingMode.HALF_UP);
    }
}
