package com.daniel.dailyView.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record KlineCandle(
        Instant openTime,
        Instant closeTime,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closePrice,
        BigDecimal volume,
        BigDecimal quoteVolume,
        Integer tradeCount
) {

    public KlineCandle(BigDecimal highPrice, BigDecimal lowPrice) {
        this(null, null, null, highPrice, lowPrice, null, null, null, null);
    }
}
