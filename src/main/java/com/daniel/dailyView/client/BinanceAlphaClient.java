package com.daniel.dailyView.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

import com.daniel.dailyView.config.AlphaFuturesProperties;
import com.daniel.dailyView.domain.AlphaToken;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;

@Component
public class BinanceAlphaClient {

    private static final String TOKEN_LIST_PATH =
            "/bapi/defi/v1/public/wallet-direct/buw/wallet/cex/alpha/all/token/list";

    private final JsonHttpClient jsonHttpClient;
    private final AlphaFuturesProperties properties;

    public BinanceAlphaClient(JsonHttpClient jsonHttpClient, AlphaFuturesProperties properties) {
        this.jsonHttpClient = jsonHttpClient;
        this.properties = properties;
    }

    public List<AlphaToken> fetchAlphaTokens() {
        AlphaTokenListResponse response = jsonHttpClient.get(
                properties.getBinanceAlphaBaseUrl() + TOKEN_LIST_PATH,
                new TypeReference<>() {
                });

        if (response == null || response.data() == null) {
            return List.of();
        }

        return response.data().stream()
                .map(this::toDomain)
                .toList();
    }

    private AlphaToken toDomain(AlphaTokenItemResponse response) {
        Instant listingTime = response.listingTime() == null ? null : Instant.ofEpochMilli(response.listingTime());
        return new AlphaToken(
                response.alphaId(),
                response.symbol(),
                response.cexCoinName(),
                response.chainId(),
                response.contractAddress(),
                listingTime,
                Boolean.TRUE.equals(response.fullyDelisted()),
                Boolean.TRUE.equals(response.offsell()),
                parseDecimal(response.circulatingSupply()),
                parseDecimal(response.totalSupply()),
                response.holders()
        );
    }

    private BigDecimal parseDecimal(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        return new BigDecimal(rawValue);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AlphaTokenListResponse(
            String code,
            List<AlphaTokenItemResponse> data
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AlphaTokenItemResponse(
            String alphaId,
            String symbol,
            String cexCoinName,
            String chainId,
            String contractAddress,
            Long listingTime,
            Boolean fullyDelisted,
            Boolean offsell,
            String circulatingSupply,
            String totalSupply,
            String holders
    ) {
    }
}
