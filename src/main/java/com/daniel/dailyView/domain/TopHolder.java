package com.daniel.dailyView.domain;

import java.math.BigDecimal;

public record TopHolder(
        String walletAddress,
        BigDecimal amount
) {
}
