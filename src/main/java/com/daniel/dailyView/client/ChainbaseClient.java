package com.daniel.dailyView.client;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import org.springframework.stereotype.Component;

import com.daniel.dailyView.config.AlphaFuturesProperties;
import com.daniel.dailyView.domain.TopHolder;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;

@Component
public class ChainbaseClient {

    private static final String TOP_HOLDERS_PATH = "/v1/token/top-holders";

    private final JsonHttpClient jsonHttpClient;
    private final AlphaFuturesProperties properties;

    public ChainbaseClient(JsonHttpClient jsonHttpClient, AlphaFuturesProperties properties) {
        this.jsonHttpClient = jsonHttpClient;
        this.properties = properties;
    }

    public boolean supportsChain(String chainId) {
        return parseChainId(chainId).isPresent();
    }

    public List<TopHolder> fetchTopHolders(String chainId, String contractAddress, int limit) {
        OptionalInt numericChainId = parseChainId(chainId);
        if (numericChainId.isEmpty()) {
            return List.of();
        }

        String url = properties.getChainbaseBaseUrl()
                + TOP_HOLDERS_PATH
                + "?chain_id=" + numericChainId.getAsInt()
                + "&contract_address=" + encode(contractAddress)
                + "&page=1"
                + "&limit=" + limit;

        ChainbaseTopHoldersResponse response = jsonHttpClient.get(
                url,
                Map.of(
                        "x-api-key", properties.getChainbaseApiKey(),
                        "accept", "application/json"
                ),
                new TypeReference<>() {
                });

        if (response == null || response.data() == null) {
            return List.of();
        }

        return response.data().stream()
                .map(holder -> new TopHolder(holder.walletAddress(), new BigDecimal(holder.amount())))
                .toList();
    }

    private OptionalInt parseChainId(String chainId) {
        if (chainId == null || chainId.isBlank()) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Integer.parseInt(chainId));
        } catch (NumberFormatException ex) {
            return OptionalInt.empty();
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChainbaseTopHoldersResponse(List<ChainbaseTopHolderItem> data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChainbaseTopHolderItem(
            String walletAddress,
            String amount
    ) {
    }
}
