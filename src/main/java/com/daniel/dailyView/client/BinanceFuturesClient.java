package com.daniel.dailyView.client;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Component;

import com.daniel.dailyView.config.AlphaFuturesProperties;
import com.daniel.dailyView.domain.FuturesContract;
import com.daniel.dailyView.domain.KlineCandle;
import com.daniel.dailyView.domain.OpenInterestPoint;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

@Component
public class BinanceFuturesClient {

    private static final String EXCHANGE_INFO_PATH = "/fapi/v1/exchangeInfo";
    private static final String OPEN_INTEREST_HISTORY_PATH = "/futures/data/openInterestHist";
    private static final String KLINE_PATH = "/fapi/v1/klines";

    private final JsonHttpClient jsonHttpClient;
    private final AlphaFuturesProperties properties;

    public BinanceFuturesClient(JsonHttpClient jsonHttpClient, AlphaFuturesProperties properties) {
        this.jsonHttpClient = jsonHttpClient;
        this.properties = properties;
    }

    public Map<String, FuturesContract> fetchUsdtPerpetualContractsByBaseAsset() {
        ExchangeInfoResponse response = jsonHttpClient.get(
                properties.getBinanceFuturesBaseUrl() + EXCHANGE_INFO_PATH,
                new TypeReference<>() {
                });

        if (response == null || response.symbols() == null) {
            return Map.of();
        }

        return response.symbols().stream()
                .filter(symbol -> "USDT".equals(symbol.quoteAsset()))
                .filter(symbol -> "PERPETUAL".equals(symbol.contractType()))
                .filter(symbol -> "TRADING".equals(symbol.status()))
                .map(symbol -> new FuturesContract(
                        symbol.symbol(),
                        symbol.baseAsset(),
                        symbol.quoteAsset(),
                        symbol.contractType(),
                        symbol.status()))
                .collect(Collectors.toMap(
                        FuturesContract::baseAsset,
                        contract -> contract,
                        (left, right) -> left
                ));
    }

    public BigDecimal fetchLatestOpenInterestValueUsdt(String symbol, String period) {
        return fetchOpenInterestHistory(symbol, period, 1).stream()
                .findFirst()
                .map(OpenInterestPoint::openInterestValueUsdt)
                .orElse(BigDecimal.ZERO);
    }

    public List<OpenInterestPoint> fetchOpenInterestHistory(String symbol, String period, int limit) {
        String url = properties.getBinanceFuturesBaseUrl()
                + OPEN_INTEREST_HISTORY_PATH
                + "?symbol=" + encode(symbol)
                + "&period=" + encode(period)
                + "&limit=" + limit;

        List<OpenInterestHistoryResponse> response = jsonHttpClient.get(url, new TypeReference<>() {
        });

        return response.stream()
                .map(item -> new OpenInterestPoint(
                        new BigDecimal(item.sumOpenInterestValue()),
                        Instant.ofEpochMilli(item.timestamp())))
                .sorted(Comparator.comparing(OpenInterestPoint::timestamp))
                .toList();
    }

    public List<KlineCandle> fetchDailyKlines(String symbol, String interval, int limit) {
        String url = properties.getBinanceFuturesBaseUrl()
                + KLINE_PATH
                + "?symbol=" + encode(symbol)
                + "&interval=" + encode(interval)
                + "&limit=" + limit;

        JsonNode response = jsonHttpClient.get(url, new TypeReference<>() {
        });

        if (!response.isArray()) {
            return List.of();
        }

        return StreamSupport.stream(response.spliterator(), false)
                .filter(JsonNode::isArray)
                .filter(node -> node.size() >= 11)
                .map(node -> new KlineCandle(
                        Instant.ofEpochMilli(node.get(0).asLong()),
                        Instant.ofEpochMilli(node.get(6).asLong()),
                        new BigDecimal(node.get(1).asText()),
                        new BigDecimal(node.get(2).asText()),
                        new BigDecimal(node.get(3).asText()),
                        new BigDecimal(node.get(4).asText()),
                        new BigDecimal(node.get(5).asText()),
                        new BigDecimal(node.get(7).asText()),
                        node.get(8).asInt()))
                .toList();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ExchangeInfoResponse(List<ExchangeSymbolResponse> symbols) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ExchangeSymbolResponse(
            String symbol,
            String baseAsset,
            String quoteAsset,
            String contractType,
            String status
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenInterestHistoryResponse(
            String sumOpenInterestValue,
            Long timestamp
    ) {
    }
}
