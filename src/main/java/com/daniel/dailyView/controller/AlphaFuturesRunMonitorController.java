package com.daniel.dailyView.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.daniel.dailyView.dto.AlphaFuturesRunDetailView;
import com.daniel.dailyView.dto.AlphaFuturesRunView;
import com.daniel.dailyView.service.AlphaFuturesRunExplorerService;

@RestController
@RequestMapping("/api/monitor/alpha-futures")
public class AlphaFuturesRunMonitorController {

    private final AlphaFuturesRunExplorerService alphaFuturesRunExplorerService;

    public AlphaFuturesRunMonitorController(AlphaFuturesRunExplorerService alphaFuturesRunExplorerService) {
        this.alphaFuturesRunExplorerService = alphaFuturesRunExplorerService;
    }

    @GetMapping("/runs")
    public List<AlphaFuturesRunView> listRuns(@RequestParam(defaultValue = "12") int limit) {
        return alphaFuturesRunExplorerService.listRecentRuns(limit);
    }

    @GetMapping("/runs/{runId}")
    public AlphaFuturesRunDetailView getRunDetail(@PathVariable long runId) {
        return alphaFuturesRunExplorerService.getRunDetail(runId);
    }
}
