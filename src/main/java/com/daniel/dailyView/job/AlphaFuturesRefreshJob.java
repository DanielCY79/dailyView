package com.daniel.dailyView.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.daniel.dailyView.service.AlphaFuturesSnapshotService;

@Component
public class AlphaFuturesRefreshJob {

    private static final Logger log = LoggerFactory.getLogger(AlphaFuturesRefreshJob.class);

    private final AlphaFuturesSnapshotService alphaFuturesSnapshotService;

    public AlphaFuturesRefreshJob(AlphaFuturesSnapshotService alphaFuturesSnapshotService) {
        this.alphaFuturesSnapshotService = alphaFuturesSnapshotService;
    }

    @Scheduled(
            cron = "${app.screener.alpha-futures.refresh-cron}",
            zone = "${app.screener.alpha-futures.refresh-zone}")
    public void refreshHourlySnapshot() {
        if (!alphaFuturesSnapshotService.isRefreshEnabled()) {
            return;
        }
        try {
            alphaFuturesSnapshotService.refreshDefaultSnapshot("scheduled-hourly");
        } catch (RuntimeException ex) {
            log.error("Scheduled alpha futures refresh failed.", ex);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmupSnapshotOnStartup() {
        if (!alphaFuturesSnapshotService.isRefreshEnabled() || !alphaFuturesSnapshotService.isRefreshRunOnStartup()) {
            log.info("Skip alpha futures startup refresh because refresh is disabled or startup warmup is turned off.");
            return;
        }
        try {
            alphaFuturesSnapshotService.refreshDefaultSnapshotIfEmpty("startup");
        } catch (RuntimeException ex) {
            log.error("Startup alpha futures refresh failed.", ex);
        }
    }
}
