package com.daniel.dailyView.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.stereotype.Service;

import com.daniel.dailyView.config.AlphaFuturesProperties;
import com.daniel.dailyView.config.ConnectivityHealthProperties;
import com.daniel.dailyView.config.HttpClientProxyProperties;
import com.daniel.dailyView.dto.ConnectivityHealthNode;
import com.daniel.dailyView.dto.ConnectivityHealthResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ConnectivityHealthService {

    private static final String STATUS_UP = "UP";
    private static final String STATUS_DOWN = "DOWN";
    private static final String STATUS_UNKNOWN = "UNKNOWN";

    private static final String BINANCE_ALPHA_PATH =
            "/bapi/defi/v1/public/wallet-direct/buw/wallet/cex/alpha/all/token/list";
    private static final String BINANCE_FUTURES_EXCHANGE_INFO_PATH = "/fapi/v1/exchangeInfo";
    private static final String CHAINBASE_TOP_HOLDERS_PATH = "/v1/token/top-holders";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final DataSource dataSource;
    private final HttpClientProxyProperties proxyProperties;
    private final AlphaFuturesProperties alphaFuturesProperties;
    private final ConnectivityHealthProperties connectivityHealthProperties;

    public ConnectivityHealthService(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            DataSource dataSource,
            HttpClientProxyProperties proxyProperties,
            AlphaFuturesProperties alphaFuturesProperties,
            ConnectivityHealthProperties connectivityHealthProperties) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.dataSource = dataSource;
        this.proxyProperties = proxyProperties;
        this.alphaFuturesProperties = alphaFuturesProperties;
        this.connectivityHealthProperties = connectivityHealthProperties;
    }

    public ConnectivityHealthResponse checkAll() {
        ConnectivityHealthNode proxy = checkProxy();
        ConnectivityHealthNode mysql = checkMysql();
        ConnectivityHealthNode binance = checkBinance();
        ConnectivityHealthNode chainbase = checkChainbase();

        Map<String, ConnectivityHealthNode> components = new LinkedHashMap<>();
        components.put("proxy", proxy);
        components.put("mysql", mysql);
        components.put("binance", binance);
        components.put("chainbase", chainbase);

        return new ConnectivityHealthResponse(resolveOverallStatus(components), Instant.now(), components);
    }

    private ConnectivityHealthNode checkProxy() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("enabled", proxyProperties.isEnabled());
        details.put("host", proxyProperties.getHost());
        details.put("port", proxyProperties.getPort());

        if (!proxyProperties.isEnabled()) {
            return new ConnectivityHealthNode(STATUS_UNKNOWN, details, Map.of());
        }

        long startedAt = System.nanoTime();
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new InetSocketAddress(proxyProperties.getHost(), proxyProperties.getPort()),
                    connectivityHealthProperties.getTimeoutSeconds() * 1000);
            details.put("connectTimeMs", elapsedMillis(startedAt));
            return new ConnectivityHealthNode(STATUS_UP, details, Map.of());
        } catch (IOException ex) {
            details.put("connectTimeMs", elapsedMillis(startedAt));
            details.put("error", compactError(ex));
            return new ConnectivityHealthNode(STATUS_DOWN, details, Map.of());
        }
    }

    private ConnectivityHealthNode checkMysql() {
        Map<String, Object> details = new LinkedHashMap<>();
        long startedAt = System.nanoTime();

        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(connectivityHealthProperties.getTimeoutSeconds());
            DatabaseMetaData metaData = connection.getMetaData();
            details.put("valid", valid);
            details.put("databaseProduct", metaData.getDatabaseProductName());
            details.put("databaseVersion", metaData.getDatabaseProductVersion());
            details.put("url", metaData.getURL());
            details.put("connectTimeMs", elapsedMillis(startedAt));
            return new ConnectivityHealthNode(valid ? STATUS_UP : STATUS_DOWN, details, Map.of());
        } catch (SQLException ex) {
            details.put("connectTimeMs", elapsedMillis(startedAt));
            details.put("error", compactError(ex));
            return new ConnectivityHealthNode(STATUS_DOWN, details, Map.of());
        }
    }

    private ConnectivityHealthNode checkBinance() {
        ConnectivityHealthNode alpha = checkBinanceAlpha();
        ConnectivityHealthNode futures = checkBinanceFutures();

        Map<String, ConnectivityHealthNode> components = new LinkedHashMap<>();
        components.put("alpha", alpha);
        components.put("futures", futures);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("proxyEnabled", proxyProperties.isEnabled());
        details.put("alphaBaseUrl", alphaFuturesProperties.getBinanceAlphaBaseUrl());
        details.put("futuresBaseUrl", alphaFuturesProperties.getBinanceFuturesBaseUrl());

        String status = STATUS_UP;
        if (!STATUS_UP.equals(alpha.status()) || !STATUS_UP.equals(futures.status())) {
            status = STATUS_DOWN;
        }

        return new ConnectivityHealthNode(status, details, components);
    }

    private ConnectivityHealthNode checkBinanceAlpha() {
        String url = alphaFuturesProperties.getBinanceAlphaBaseUrl() + BINANCE_ALPHA_PATH;
        long startedAt = System.nanoTime();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("url", url);
        details.put("proxyEnabled", proxyProperties.isEnabled());

        try {
            JsonNode response = getJson(url, Map.of());
            details.put("connectTimeMs", elapsedMillis(startedAt));
            details.put("code", response.path("code").asText());
            details.put("tokenCount", response.path("data").isArray() ? response.path("data").size() : 0);
            return new ConnectivityHealthNode(STATUS_UP, details, Map.of());
        } catch (Exception ex) {
            details.put("connectTimeMs", elapsedMillis(startedAt));
            details.put("error", compactError(ex));
            return new ConnectivityHealthNode(STATUS_DOWN, details, Map.of());
        }
    }

    private ConnectivityHealthNode checkBinanceFutures() {
        String url = alphaFuturesProperties.getBinanceFuturesBaseUrl() + BINANCE_FUTURES_EXCHANGE_INFO_PATH;
        long startedAt = System.nanoTime();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("url", url);
        details.put("proxyEnabled", proxyProperties.isEnabled());

        try {
            JsonNode response = getJson(url, Map.of());
            details.put("connectTimeMs", elapsedMillis(startedAt));
            details.put("symbolCount", response.path("symbols").isArray() ? response.path("symbols").size() : 0);
            return new ConnectivityHealthNode(STATUS_UP, details, Map.of());
        } catch (Exception ex) {
            details.put("connectTimeMs", elapsedMillis(startedAt));
            details.put("error", compactError(ex));
            return new ConnectivityHealthNode(STATUS_DOWN, details, Map.of());
        }
    }

    private ConnectivityHealthNode checkChainbase() {
        String url = alphaFuturesProperties.getChainbaseBaseUrl()
                + CHAINBASE_TOP_HOLDERS_PATH
                + "?chain_id=" + connectivityHealthProperties.getChainbaseSampleChainId()
                + "&contract_address=" + connectivityHealthProperties.getChainbaseSampleContractAddress()
                + "&page=1&limit=3";

        long startedAt = System.nanoTime();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("url", url);
        details.put("proxyEnabled", proxyProperties.isEnabled());
        details.put("sampleChainId", connectivityHealthProperties.getChainbaseSampleChainId());
        details.put("sampleContractAddress", connectivityHealthProperties.getChainbaseSampleContractAddress());
        details.put("apiKeyConfigured", alphaFuturesProperties.getChainbaseApiKey() != null
                && !alphaFuturesProperties.getChainbaseApiKey().isBlank());
        details.put("usingDemoKey", "demo".equalsIgnoreCase(alphaFuturesProperties.getChainbaseApiKey()));

        try {
            JsonNode response = getJson(url, Map.of(
                    "x-api-key", alphaFuturesProperties.getChainbaseApiKey(),
                    "accept", "application/json"
            ));
            details.put("connectTimeMs", elapsedMillis(startedAt));
            details.put("code", response.path("code").asInt());
            details.put("holderCount", response.path("data").isArray() ? response.path("data").size() : 0);
            return new ConnectivityHealthNode(STATUS_UP, details, Map.of());
        } catch (Exception ex) {
            details.put("connectTimeMs", elapsedMillis(startedAt));
            details.put("error", compactError(ex));
            return new ConnectivityHealthNode(STATUS_DOWN, details, Map.of());
        }
    }

    private JsonNode getJson(String url, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(connectivityHealthProperties.getTimeoutSeconds()))
                .header("Accept", "application/json")
                .header("User-Agent", "dailyView-health/0.0.1");

        headers.forEach(builder::header);

        HttpResponse<String> response = httpClient.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    private String resolveOverallStatus(Map<String, ConnectivityHealthNode> components) {
        return components.values().stream()
                .allMatch(component -> STATUS_UP.equals(component.status()))
                ? STATUS_UP
                : STATUS_DOWN;
    }

    private long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    private String compactError(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        String compact = ex.getClass().getSimpleName() + ": " + message;
        return compact.length() <= 500 ? compact : compact.substring(0, 500);
    }
}
