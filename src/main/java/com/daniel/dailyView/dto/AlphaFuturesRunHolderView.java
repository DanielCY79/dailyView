package com.daniel.dailyView.dto;

import java.math.BigDecimal;

public record AlphaFuturesRunHolderView(
        String alphaId,
        String alphaSymbol,
        String futuresSymbol,
        String chainId,
        String contractAddress,
        int topN,
        int rankNo,
        String walletAddress,
        BigDecimal amount
) {
}
