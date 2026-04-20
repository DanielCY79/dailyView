package com.daniel.dailyView.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.daniel.dailyView.client.BinanceAlphaClient;
import com.daniel.dailyView.client.BinanceFuturesClient;
import com.daniel.dailyView.config.AlphaFuturesProperties;
import com.daniel.dailyView.domain.AlphaFuturesUniverseEntry;
import com.daniel.dailyView.domain.AlphaToken;
import com.daniel.dailyView.domain.FuturesContract;

@Service
public class AlphaTokenUniverseService {

    private final BinanceAlphaClient binanceAlphaClient;
    private final BinanceFuturesClient binanceFuturesClient;
    private final AlphaFuturesProperties properties;

    public AlphaTokenUniverseService(
            BinanceAlphaClient binanceAlphaClient,
            BinanceFuturesClient binanceFuturesClient,
            AlphaFuturesProperties properties) {
        this.binanceAlphaClient = binanceAlphaClient;
        this.binanceFuturesClient = binanceFuturesClient;
        this.properties = properties;
    }

    public List<AlphaToken> fetchAlphaTokens() {
        return binanceAlphaClient.fetchAlphaTokens();
    }

    public List<AlphaFuturesUniverseEntry> loadMappedUniverse(List<AlphaToken> alphaTokens) {
        Map<String, FuturesContract> futuresContracts = binanceFuturesClient.fetchUsdtPerpetualContractsByBaseAsset();

        return alphaTokens.stream()
                .filter(this::isEligibleAlphaToken)
                .filter(token -> futuresContracts.containsKey(token.cexCoinName()))
                .map(token -> new AlphaFuturesUniverseEntry(token, futuresContracts.get(token.cexCoinName())))
                .toList();
    }

    private boolean isEligibleAlphaToken(AlphaToken token) {
        if (token == null || token.cexCoinName() == null || token.cexCoinName().isBlank()) {
            return false;
        }
        if (properties.isExcludeFullyDelisted() && token.fullyDelisted()) {
            return false;
        }
        return !properties.isExcludeOffsell() || !token.offsell();
    }
}
