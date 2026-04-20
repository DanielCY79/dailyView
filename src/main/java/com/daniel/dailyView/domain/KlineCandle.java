package com.daniel.dailyView.domain;

import java.math.BigDecimal;

public record KlineCandle(
        BigDecimal highPrice,
        BigDecimal lowPrice
) {
}
