package com.daniel.dailyView.domain;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import com.daniel.dailyView.dto.AlphaFuturesCandidateView;
import com.daniel.dailyView.dto.AlphaFuturesRulesView;
import com.daniel.dailyView.dto.AlphaFuturesScreenResponse;
import com.daniel.dailyView.dto.AlphaFuturesSummaryView;

public record AlphaFuturesScreeningReport(
        Instant generatedAt,
        AlphaFuturesRules rules,
        List<AlphaToken> alphaTokens,
        List<FuturesContract> futuresContracts,
        List<AlphaFuturesCandidateReport> candidateReports
) {

    public AlphaFuturesScreenResponse toResponse(boolean includeRejected) {
        List<AlphaFuturesCandidateReport> sortedReports = candidateReports.stream()
                .sorted(Comparator.comparing(
                                AlphaFuturesCandidateReport::currentOpenInterestUsdt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(report -> report.universeEntry().alphaToken().cexCoinName(),
                                Comparator.nullsLast(String::compareTo)))
                .toList();

        List<AlphaFuturesCandidateReport> returnedReports = includeRejected
                ? sortedReports
                : sortedReports.stream().filter(AlphaFuturesCandidateReport::passed).toList();

        int passedCount = (int) sortedReports.stream().filter(AlphaFuturesCandidateReport::passed).count();
        int rejectedCount = sortedReports.size() - passedCount;

        AlphaFuturesRulesView rulesView = new AlphaFuturesRulesView(
                rules.minOpenInterestUsdt(),
                rules.maxMonthlyPumpPct(),
                rules.minTop10HoldingRatioPct(),
                rules.minWeeklyAverageOiRatio(),
                rules.minWeeklyPeakOiRatio(),
                rules.recentWindowDays()
        );

        AlphaFuturesSummaryView summary = new AlphaFuturesSummaryView(
                alphaTokens.size(),
                sortedReports.size(),
                passedCount,
                rejectedCount,
                returnedReports.size()
        );

        List<AlphaFuturesCandidateView> views = returnedReports.stream()
                .map(AlphaFuturesCandidateReport::toView)
                .toList();

        return new AlphaFuturesScreenResponse(generatedAt, rulesView, summary, views);
    }
}
