package com.daniel.dailyView.dto;

public record AlphaFuturesRunStatsView(
        int alphaTokenSnapshotCount,
        int futuresContractSnapshotCount,
        int screenResultCount,
        int payloadCount,
        int holderDetailCount,
        int openInterestPointCount,
        int priceKlineCount
) {
}
