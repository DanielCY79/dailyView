package com.daniel.dailyView.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.daniel.dailyView.config.AlphaFuturesProperties;
import com.daniel.dailyView.domain.AlphaFuturesScreeningReport;
import com.daniel.dailyView.dto.AlphaFuturesCandidateView;
import com.daniel.dailyView.dto.AlphaFuturesScreenRequest;
import com.daniel.dailyView.dto.AlphaFuturesScreenResponse;
import com.daniel.dailyView.dto.AlphaFuturesSummaryView;

@Service
public class AlphaFuturesSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(AlphaFuturesSnapshotService.class);

    private final AlphaFuturesProperties properties;
    private final AlphaFuturesScreenerService alphaFuturesScreenerService;
    private final AlphaFuturesPersistenceService alphaFuturesPersistenceService;
    private final AtomicReference<AlphaFuturesScreenResponse> latestSnapshot = new AtomicReference<>();
    private final Object refreshMonitor = new Object();

    public AlphaFuturesSnapshotService(
            AlphaFuturesProperties properties,
            AlphaFuturesScreenerService alphaFuturesScreenerService,
            AlphaFuturesPersistenceService alphaFuturesPersistenceService) {
        this.properties = properties;
        this.alphaFuturesScreenerService = alphaFuturesScreenerService;
        this.alphaFuturesPersistenceService = alphaFuturesPersistenceService;
    }

    public AlphaFuturesScreenResponse getLatestSnapshot(boolean includeRejected) {
        AlphaFuturesScreenResponse snapshot = latestSnapshot.get();
        if (snapshot == null) {
            snapshot = refreshDefaultSnapshotIfEmpty("on-demand-miss");
        }
        return includeRejected ? snapshot : passedOnly(snapshot);
    }

    public AlphaFuturesScreenResponse refreshDefaultSnapshot(String trigger) {
        synchronized (refreshMonitor) {
            AlphaFuturesScreeningReport report = alphaFuturesScreenerService.buildReport(defaultRequest());
            alphaFuturesPersistenceService.persistDefaultRun(trigger, report);
            AlphaFuturesScreenResponse response = report.toResponse(true);
            latestSnapshot.set(response);
            log.info(
                    "Alpha futures snapshot refreshed. trigger={}, generatedAt={}, returnedCandidates={}, passedCandidates={}",
                    trigger,
                    response.generatedAt(),
                    response.summary().returnedCandidates(),
                    response.summary().passedCandidates()
            );
            return response;
        }
    }

    public AlphaFuturesScreenResponse refreshDefaultSnapshotIfEmpty(String trigger) {
        synchronized (refreshMonitor) {
            AlphaFuturesScreenResponse snapshot = latestSnapshot.get();
            if (snapshot != null) {
                return snapshot;
            }
            return refreshDefaultSnapshot(trigger);
        }
    }

    public boolean usesDefaultRules(
            BigDecimal minOpenInterestUsdt,
            BigDecimal maxMonthlyPumpPct,
            BigDecimal minTop10HoldingRatioPct,
            BigDecimal minWeeklyAverageOiRatio,
            BigDecimal minWeeklyPeakOiRatio) {
        return minOpenInterestUsdt == null
                && maxMonthlyPumpPct == null
                && minTop10HoldingRatioPct == null
                && minWeeklyAverageOiRatio == null
                && minWeeklyPeakOiRatio == null;
    }

    public boolean isRefreshEnabled() {
        return properties.isRefreshEnabled();
    }

    public boolean isRefreshRunOnStartup() {
        return properties.isRefreshRunOnStartup();
    }

    private AlphaFuturesScreenRequest defaultRequest() {
        return new AlphaFuturesScreenRequest(
                true,
                null,
                null,
                null,
                null,
                null
        );
    }

    private AlphaFuturesScreenResponse passedOnly(AlphaFuturesScreenResponse snapshot) {
        List<AlphaFuturesCandidateView> passedCandidates = snapshot.candidates().stream()
                .filter(AlphaFuturesCandidateView::passed)
                .toList();

        AlphaFuturesSummaryView summary = new AlphaFuturesSummaryView(
                snapshot.summary().totalAlphaTokens(),
                snapshot.summary().mappedCandidates(),
                snapshot.summary().passedCandidates(),
                snapshot.summary().rejectedCandidates(),
                passedCandidates.size()
        );

        return new AlphaFuturesScreenResponse(
                snapshot.generatedAt(),
                snapshot.rules(),
                summary,
                passedCandidates
        );
    }
}
