package com.daniel.dailyView.dto;

import java.util.List;

public record AlphaFuturesRunDetailView(
        AlphaFuturesRunView run,
        AlphaFuturesRunStatsView stats,
        List<AlphaFuturesRunCandidateView> candidates,
        List<AlphaFuturesRunHolderView> topHolders,
        List<AlphaFuturesRunPayloadView> payloads
) {
}
