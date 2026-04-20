package com.daniel.dailyView.domain;

public record FuturesContract(
        String symbol,
        String baseAsset,
        String quoteAsset,
        String contractType,
        String status
) {
}
