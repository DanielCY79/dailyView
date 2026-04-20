package com.daniel.dailyView.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record AlphaToken(
        String alphaId,
        String symbol,
        String cexCoinName,
        String chainId,
        String contractAddress,
        Instant listingTime,
        boolean fullyDelisted,
        boolean offsell,
        BigDecimal circulatingSupply,
        BigDecimal totalSupply,
        String holders
) {
}
