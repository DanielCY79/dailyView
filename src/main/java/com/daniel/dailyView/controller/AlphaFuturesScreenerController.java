package com.daniel.dailyView.controller;

import java.math.BigDecimal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.daniel.dailyView.dto.AlphaFuturesScreenRequest;
import com.daniel.dailyView.dto.AlphaFuturesScreenResponse;
import com.daniel.dailyView.service.AlphaFuturesScreenerService;
import com.daniel.dailyView.service.AlphaFuturesSnapshotService;

@RestController
@RequestMapping("/api/screener")
public class AlphaFuturesScreenerController {

    private final AlphaFuturesScreenerService alphaFuturesScreenerService;
    private final AlphaFuturesSnapshotService alphaFuturesSnapshotService;

    public AlphaFuturesScreenerController(
            AlphaFuturesScreenerService alphaFuturesScreenerService,
            AlphaFuturesSnapshotService alphaFuturesSnapshotService) {
        this.alphaFuturesScreenerService = alphaFuturesScreenerService;
        this.alphaFuturesSnapshotService = alphaFuturesSnapshotService;
    }

    @GetMapping("/alpha-futures")
    public AlphaFuturesScreenResponse screenAlphaFutures(
            @RequestParam(defaultValue = "false") boolean includeRejected,
            @RequestParam(defaultValue = "false") boolean forceRefresh,
            @RequestParam(required = false) BigDecimal minOpenInterestUsdt,
            @RequestParam(required = false) BigDecimal maxMonthlyPumpPct,
            @RequestParam(required = false) BigDecimal minTop10HoldingRatioPct,
            @RequestParam(required = false) BigDecimal minWeeklyAverageOiRatio,
            @RequestParam(required = false) BigDecimal minWeeklyPeakOiRatio) {

        AlphaFuturesScreenRequest request = new AlphaFuturesScreenRequest(
                includeRejected,
                minOpenInterestUsdt,
                maxMonthlyPumpPct,
                minTop10HoldingRatioPct,
                minWeeklyAverageOiRatio,
                minWeeklyPeakOiRatio
        );

        if (!forceRefresh && alphaFuturesSnapshotService.usesDefaultRules(
                minOpenInterestUsdt,
                maxMonthlyPumpPct,
                minTop10HoldingRatioPct,
                minWeeklyAverageOiRatio,
                minWeeklyPeakOiRatio)) {
            return alphaFuturesSnapshotService.getLatestSnapshot(includeRejected);
        }

        if (forceRefresh && alphaFuturesSnapshotService.usesDefaultRules(
                minOpenInterestUsdt,
                maxMonthlyPumpPct,
                minTop10HoldingRatioPct,
                minWeeklyAverageOiRatio,
                minWeeklyPeakOiRatio)) {
            AlphaFuturesScreenResponse refreshed = alphaFuturesSnapshotService.refreshDefaultSnapshot("manual-force");
            return includeRejected ? refreshed : alphaFuturesSnapshotService.getLatestSnapshot(false);
        }

        return alphaFuturesScreenerService.screen(request);
    }
}
