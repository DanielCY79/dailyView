package com.daniel.dailyView.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import com.daniel.dailyView.config.AlphaFuturesProperties;
import com.daniel.dailyView.domain.AlphaFuturesCandidateReport;
import com.daniel.dailyView.domain.AlphaFuturesScreeningReport;
import com.daniel.dailyView.domain.AlphaToken;
import com.daniel.dailyView.domain.FuturesContract;
import com.daniel.dailyView.domain.KlineCandle;
import com.daniel.dailyView.domain.OpenInterestPoint;
import com.daniel.dailyView.domain.TopHolder;
import com.daniel.dailyView.dto.AlphaFuturesScreenResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AlphaFuturesPersistenceService {

    private static final String JOB_NAME = "alpha-futures-screener";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final AlphaFuturesProperties properties;

    public AlphaFuturesPersistenceService(
            NamedParameterJdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            AlphaFuturesProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public long persistDefaultRun(String triggerType, AlphaFuturesScreeningReport report) {
        long runId = insertRun(triggerType, report);

        try {
            persistAlphaTokenSnapshots(runId, report.alphaTokens());
            persistFuturesContractSnapshots(runId, report.futuresContracts());
            persistScreenResults(runId, report.candidateReports());
            refreshCurrentScreenResults(runId, report.candidateReports(), report.generatedAt());
            persistOpenInterestHistory(runId, report.candidateReports());
            persistPriceKlines(runId, report.candidateReports());
            persistHolderSnapshots(runId, report.candidateReports());
            persistHolderDetails(runId, report.candidateReports());
            persistPayloadArchive(runId, report);
            updateRunSuccess(runId, report);
            return runId;
        } catch (RuntimeException ex) {
            updateRunFailure(runId, ex);
            throw ex;
        }
    }

    private long insertRun(String triggerType, AlphaFuturesScreeningReport report) {
        String sql = """
                INSERT INTO screen_run (
                    job_name, trigger_type, status, started_at, rules_json,
                    total_alpha_tokens, mapped_candidates, passed_candidates, rejected_candidates
                ) VALUES (
                    :jobName, :triggerType, :status, :startedAt, :rulesJson,
                    :totalAlphaTokens, :mappedCandidates, :passedCandidates, :rejectedCandidates
                )
                """;

        AlphaFuturesScreenResponse response = report.toResponse(true);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("jobName", JOB_NAME)
                .addValue("triggerType", triggerType)
                .addValue("status", "RUNNING")
                .addValue("startedAt", toTimestamp(report.generatedAt()))
                .addValue("rulesJson", toJson(report.rules()))
                .addValue("totalAlphaTokens", response.summary().totalAlphaTokens())
                .addValue("mappedCandidates", response.summary().mappedCandidates())
                .addValue("passedCandidates", response.summary().passedCandidates())
                .addValue("rejectedCandidates", response.summary().rejectedCandidates());

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder, new String[] {"id"});
        return keyHolder.getKey().longValue();
    }

    private void updateRunSuccess(long runId, AlphaFuturesScreeningReport report) {
        Instant endedAt = Instant.now();
        int durationMs = Math.toIntExact(Duration.between(report.generatedAt(), endedAt).toMillis());

        jdbcTemplate.update("""
                        UPDATE screen_run
                        SET status = :status,
                            ended_at = :endedAt,
                            duration_ms = :durationMs
                        WHERE id = :id
                        """,
                new MapSqlParameterSource()
                        .addValue("status", "SUCCESS")
                        .addValue("endedAt", toTimestamp(endedAt))
                        .addValue("durationMs", durationMs)
                        .addValue("id", runId));
    }

    private void updateRunFailure(long runId, RuntimeException ex) {
        Instant endedAt = Instant.now();
        jdbcTemplate.update("""
                        UPDATE screen_run
                        SET status = :status,
                            ended_at = :endedAt,
                            error_message = :errorMessage
                        WHERE id = :id
                        """,
                new MapSqlParameterSource()
                        .addValue("status", "FAILED")
                        .addValue("endedAt", toTimestamp(endedAt))
                        .addValue("errorMessage", trimMessage(ex.getMessage()))
                        .addValue("id", runId));
    }

    private void persistAlphaTokenSnapshots(long runId, List<AlphaToken> alphaTokens) {
        String sql = """
                INSERT INTO alpha_token_snapshot (
                    run_id, alpha_id, symbol, cex_coin_name, chain_id, contract_address, listing_time,
                    total_supply, circulating_supply, holders, fully_delisted, offsell
                ) VALUES (
                    :runId, :alphaId, :symbol, :cexCoinName, :chainId, :contractAddress, :listingTime,
                    :totalSupply, :circulatingSupply, :holders, :fullyDelisted, :offsell
                )
                """;

        for (AlphaToken alphaToken : alphaTokens) {
            jdbcTemplate.update(sql, new MapSqlParameterSource()
                    .addValue("runId", runId)
                    .addValue("alphaId", alphaToken.alphaId())
                    .addValue("symbol", alphaToken.symbol())
                    .addValue("cexCoinName", alphaToken.cexCoinName())
                    .addValue("chainId", alphaToken.chainId())
                    .addValue("contractAddress", alphaToken.contractAddress())
                    .addValue("listingTime", toTimestamp(alphaToken.listingTime()))
                    .addValue("totalSupply", alphaToken.totalSupply())
                    .addValue("circulatingSupply", alphaToken.circulatingSupply())
                    .addValue("holders", parseLong(alphaToken.holders()))
                    .addValue("fullyDelisted", alphaToken.fullyDelisted())
                    .addValue("offsell", alphaToken.offsell()));
        }
    }

    private void persistFuturesContractSnapshots(long runId, List<FuturesContract> futuresContracts) {
        String sql = """
                INSERT INTO futures_contract_snapshot (
                    run_id, symbol, base_asset, quote_asset, contract_type, status
                ) VALUES (
                    :runId, :symbol, :baseAsset, :quoteAsset, :contractType, :status
                )
                """;

        for (FuturesContract futuresContract : futuresContracts) {
            jdbcTemplate.update(sql, new MapSqlParameterSource()
                    .addValue("runId", runId)
                    .addValue("symbol", futuresContract.symbol())
                    .addValue("baseAsset", futuresContract.baseAsset())
                    .addValue("quoteAsset", futuresContract.quoteAsset())
                    .addValue("contractType", futuresContract.contractType())
                    .addValue("status", futuresContract.status()));
        }
    }

    private void persistScreenResults(long runId, List<AlphaFuturesCandidateReport> candidateReports) {
        String sql = """
                INSERT INTO screen_result (
                    run_id, alpha_id, alpha_symbol, cex_coin_name, futures_symbol, chain_id, contract_address,
                    listing_time, current_open_interest_usdt, monthly_pump_pct, top10_holding_ratio_pct,
                    recent_week_average_oi_ratio, recent_week_peak_oi_ratio, top_holder_amount, supply_amount,
                    supply_consistent, supply_denominator_source, passed, reject_reasons_json
                ) VALUES (
                    :runId, :alphaId, :alphaSymbol, :cexCoinName, :futuresSymbol, :chainId, :contractAddress,
                    :listingTime, :currentOpenInterestUsdt, :monthlyPumpPct, :top10HoldingRatioPct,
                    :recentWeekAverageOiRatio, :recentWeekPeakOiRatio, :topHolderAmount, :supplyAmount,
                    :supplyConsistent, :supplyDenominatorSource, :passed, :rejectReasonsJson
                )
                """;

        for (AlphaFuturesCandidateReport candidateReport : candidateReports) {
            jdbcTemplate.update(sql, screenResultParams(runId, candidateReport, null));
        }
    }

    private void refreshCurrentScreenResults(
            long runId,
            List<AlphaFuturesCandidateReport> candidateReports,
            Instant generatedAt) {
        jdbcTemplate.getJdbcTemplate().update("DELETE FROM screen_result_current");

        String sql = """
                INSERT INTO screen_result_current (
                    run_id, alpha_id, alpha_symbol, cex_coin_name, futures_symbol, chain_id, contract_address,
                    listing_time, current_open_interest_usdt, monthly_pump_pct, top10_holding_ratio_pct,
                    recent_week_average_oi_ratio, recent_week_peak_oi_ratio, top_holder_amount, supply_amount,
                    supply_consistent, supply_denominator_source, passed, reject_reasons_json, last_refreshed_at
                ) VALUES (
                    :runId, :alphaId, :alphaSymbol, :cexCoinName, :futuresSymbol, :chainId, :contractAddress,
                    :listingTime, :currentOpenInterestUsdt, :monthlyPumpPct, :top10HoldingRatioPct,
                    :recentWeekAverageOiRatio, :recentWeekPeakOiRatio, :topHolderAmount, :supplyAmount,
                    :supplyConsistent, :supplyDenominatorSource, :passed, :rejectReasonsJson, :lastRefreshedAt
                )
                """;

        for (AlphaFuturesCandidateReport candidateReport : candidateReports) {
            jdbcTemplate.update(sql, screenResultParams(runId, candidateReport, generatedAt));
        }
    }

    private MapSqlParameterSource screenResultParams(
            long runId,
            AlphaFuturesCandidateReport candidateReport,
            Instant lastRefreshedAt) {
        return new MapSqlParameterSource()
                .addValue("runId", runId)
                .addValue("alphaId", candidateReport.universeEntry().alphaToken().alphaId())
                .addValue("alphaSymbol", candidateReport.universeEntry().alphaToken().symbol())
                .addValue("cexCoinName", candidateReport.universeEntry().alphaToken().cexCoinName())
                .addValue("futuresSymbol", candidateReport.universeEntry().futuresContract().symbol())
                .addValue("chainId", candidateReport.universeEntry().alphaToken().chainId())
                .addValue("contractAddress", candidateReport.universeEntry().alphaToken().contractAddress())
                .addValue("listingTime", toTimestamp(candidateReport.universeEntry().alphaToken().listingTime()))
                .addValue("currentOpenInterestUsdt", candidateReport.currentOpenInterestUsdt())
                .addValue("monthlyPumpPct", candidateReport.priceExplosionMetrics().monthlyPumpPct())
                .addValue("top10HoldingRatioPct", candidateReport.holderConcentrationMetrics().topHoldingRatioPct())
                .addValue("recentWeekAverageOiRatio", candidateReport.openInterestAnomalyMetrics().recentWeekAverageRatio())
                .addValue("recentWeekPeakOiRatio", candidateReport.openInterestAnomalyMetrics().recentWeekPeakRatio())
                .addValue("topHolderAmount", candidateReport.holderConcentrationMetrics().topHolderAmount())
                .addValue("supplyAmount", candidateReport.holderConcentrationMetrics().supplyAmount())
                .addValue("supplyConsistent", candidateReport.holderConcentrationMetrics().supplyConsistent())
                .addValue("supplyDenominatorSource", candidateReport.holderConcentrationMetrics().denominatorSource())
                .addValue("passed", candidateReport.passed())
                .addValue("rejectReasonsJson", toJson(candidateReport.rejectReasons()))
                .addValue("lastRefreshedAt", toTimestamp(lastRefreshedAt));
    }

    private void persistOpenInterestHistory(long runId, List<AlphaFuturesCandidateReport> candidateReports) {
        String sql = """
                INSERT INTO open_interest_history (
                    symbol, period_code, source_timestamp, open_interest_value_usdt, first_seen_run_id, last_seen_run_id
                ) VALUES (
                    :symbol, :periodCode, :sourceTimestamp, :openInterestValueUsdt, :firstSeenRunId, :lastSeenRunId
                )
                ON DUPLICATE KEY UPDATE
                    open_interest_value_usdt = VALUES(open_interest_value_usdt),
                    last_seen_run_id = VALUES(last_seen_run_id)
                """;

        for (AlphaFuturesCandidateReport candidateReport : candidateReports) {
            for (OpenInterestPoint point : candidateReport.openInterestHistory()) {
                jdbcTemplate.update(sql, new MapSqlParameterSource()
                        .addValue("symbol", candidateReport.universeEntry().futuresContract().symbol())
                        .addValue("periodCode", properties.getOiHistoryPeriod())
                        .addValue("sourceTimestamp", toTimestamp(point.timestamp()))
                        .addValue("openInterestValueUsdt", point.openInterestValueUsdt())
                        .addValue("firstSeenRunId", runId)
                        .addValue("lastSeenRunId", runId));
            }
        }
    }

    private void persistPriceKlines(long runId, List<AlphaFuturesCandidateReport> candidateReports) {
        String sql = """
                INSERT INTO price_kline (
                    symbol, interval_code, open_time, close_time, open_price, high_price, low_price, close_price,
                    volume, quote_volume, trade_count, first_seen_run_id, last_seen_run_id
                ) VALUES (
                    :symbol, :intervalCode, :openTime, :closeTime, :openPrice, :highPrice, :lowPrice, :closePrice,
                    :volume, :quoteVolume, :tradeCount, :firstSeenRunId, :lastSeenRunId
                )
                ON DUPLICATE KEY UPDATE
                    close_time = VALUES(close_time),
                    open_price = VALUES(open_price),
                    high_price = VALUES(high_price),
                    low_price = VALUES(low_price),
                    close_price = VALUES(close_price),
                    volume = VALUES(volume),
                    quote_volume = VALUES(quote_volume),
                    trade_count = VALUES(trade_count),
                    last_seen_run_id = VALUES(last_seen_run_id)
                """;

        for (AlphaFuturesCandidateReport candidateReport : candidateReports) {
            for (KlineCandle kline : candidateReport.priceKlines()) {
                jdbcTemplate.update(sql, new MapSqlParameterSource()
                        .addValue("symbol", candidateReport.universeEntry().futuresContract().symbol())
                        .addValue("intervalCode", properties.getPriceKlineInterval())
                        .addValue("openTime", toTimestamp(kline.openTime()))
                        .addValue("closeTime", toTimestamp(kline.closeTime()))
                        .addValue("openPrice", kline.openPrice())
                        .addValue("highPrice", kline.highPrice())
                        .addValue("lowPrice", kline.lowPrice())
                        .addValue("closePrice", kline.closePrice())
                        .addValue("volume", kline.volume())
                        .addValue("quoteVolume", kline.quoteVolume())
                        .addValue("tradeCount", kline.tradeCount())
                        .addValue("firstSeenRunId", runId)
                        .addValue("lastSeenRunId", runId));
            }
        }
    }

    private void persistHolderSnapshots(long runId, List<AlphaFuturesCandidateReport> candidateReports) {
        String sql = """
                INSERT INTO holder_concentration_snapshot (
                    run_id, alpha_id, futures_symbol, chain_id, contract_address, top_n, top_holder_amount,
                    supply_amount, holding_ratio_pct, supply_consistent, denominator_source, supported_chain,
                    scan_status, error_message
                ) VALUES (
                    :runId, :alphaId, :futuresSymbol, :chainId, :contractAddress, :topN, :topHolderAmount,
                    :supplyAmount, :holdingRatioPct, :supplyConsistent, :denominatorSource, :supportedChain,
                    :scanStatus, :errorMessage
                )
                """;

        for (AlphaFuturesCandidateReport candidateReport : candidateReports) {
            jdbcTemplate.update(sql, new MapSqlParameterSource()
                    .addValue("runId", runId)
                    .addValue("alphaId", candidateReport.universeEntry().alphaToken().alphaId())
                    .addValue("futuresSymbol", candidateReport.universeEntry().futuresContract().symbol())
                    .addValue("chainId", candidateReport.universeEntry().alphaToken().chainId())
                    .addValue("contractAddress", candidateReport.universeEntry().alphaToken().contractAddress())
                    .addValue("topN", properties.getTopHolderLimit())
                    .addValue("topHolderAmount", candidateReport.holderConcentrationMetrics().topHolderAmount())
                    .addValue("supplyAmount", candidateReport.holderConcentrationMetrics().supplyAmount())
                    .addValue("holdingRatioPct", candidateReport.holderConcentrationMetrics().topHoldingRatioPct())
                    .addValue("supplyConsistent", candidateReport.holderConcentrationMetrics().supplyConsistent())
                    .addValue("denominatorSource", candidateReport.holderConcentrationMetrics().denominatorSource())
                    .addValue("supportedChain", candidateReport.supportedChain())
                    .addValue("scanStatus", resolveHolderScanStatus(candidateReport))
                    .addValue("errorMessage", candidateReport.rejectReasons().isEmpty()
                            ? null
                            : String.join(",", candidateReport.rejectReasons())));
        }
    }

    private void persistHolderDetails(long runId, List<AlphaFuturesCandidateReport> candidateReports) {
        String sql = """
                INSERT INTO holder_detail_topn (
                    run_id, alpha_id, chain_id, contract_address, top_n, rank_no, wallet_address, amount
                ) VALUES (
                    :runId, :alphaId, :chainId, :contractAddress, :topN, :rankNo, :walletAddress, :amount
                )
                """;

        for (AlphaFuturesCandidateReport candidateReport : candidateReports) {
            List<TopHolder> topHolders = candidateReport.topHolders();
            for (int index = 0; index < topHolders.size(); index++) {
                TopHolder topHolder = topHolders.get(index);
                jdbcTemplate.update(sql, new MapSqlParameterSource()
                        .addValue("runId", runId)
                        .addValue("alphaId", candidateReport.universeEntry().alphaToken().alphaId())
                        .addValue("chainId", candidateReport.universeEntry().alphaToken().chainId())
                        .addValue("contractAddress", candidateReport.universeEntry().alphaToken().contractAddress())
                        .addValue("topN", properties.getTopHolderLimit())
                        .addValue("rankNo", index + 1)
                        .addValue("walletAddress", topHolder.walletAddress())
                        .addValue("amount", topHolder.amount()));
            }
        }
    }

    private void persistPayloadArchive(long runId, AlphaFuturesScreeningReport report) {
        insertPayload(runId, "binance", "alpha-token-list", "ALL",
                properties.getBinanceAlphaBaseUrl() + "/bapi/defi/v1/public/wallet-direct/buw/wallet/cex/alpha/all/token/list",
                Map.of(),
                report.alphaTokens(),
                true,
                null,
                report.generatedAt());

        insertPayload(runId, "binance", "futures-contracts", "ALL",
                properties.getBinanceFuturesBaseUrl() + "/fapi/v1/exchangeInfo",
                Map.of("quoteAsset", "USDT", "contractType", "PERPETUAL", "status", "TRADING"),
                report.futuresContracts(),
                true,
                null,
                report.generatedAt());

        for (AlphaFuturesCandidateReport candidateReport : report.candidateReports()) {
            String futuresSymbol = candidateReport.universeEntry().futuresContract().symbol();
            insertPayload(runId, "binance", "open-interest-history", futuresSymbol,
                    properties.getBinanceFuturesBaseUrl() + "/futures/data/openInterestHist",
                    Map.of("symbol", futuresSymbol, "period", properties.getOiHistoryPeriod(), "limit", properties.getOiHistoryLimit()),
                    candidateReport.openInterestHistory(),
                    !candidateReport.openInterestHistory().isEmpty(),
                    candidateReport.openInterestHistory().isEmpty() ? "empty_open_interest_history" : null,
                    report.generatedAt());

            insertPayload(runId, "binance", "price-kline", futuresSymbol,
                    properties.getBinanceFuturesBaseUrl() + "/fapi/v1/klines",
                    Map.of("symbol", futuresSymbol, "interval", properties.getPriceKlineInterval(), "limit",
                            properties.getMonthlyKlineLimit()),
                    candidateReport.priceKlines(),
                    !candidateReport.priceKlines().isEmpty(),
                    candidateReport.priceKlines().isEmpty() ? "empty_price_kline" : null,
                    report.generatedAt());

            String entityKey = candidateReport.universeEntry().alphaToken().alphaId();
            insertPayload(runId, "chainbase", "top-holders", entityKey,
                    properties.getChainbaseBaseUrl() + "/v1/token/top-holders",
                    Map.of(
                            "chain_id", candidateReport.universeEntry().alphaToken().chainId(),
                            "contract_address", candidateReport.universeEntry().alphaToken().contractAddress(),
                            "limit", properties.getTopHolderLimit()),
                    candidateReport.topHolders(),
                    candidateReport.supportedChain() && !candidateReport.topHolders().isEmpty(),
                    candidateReport.supportedChain() ? "empty_top_holders" : "unsupported_chain_for_holder_scan",
                    report.generatedAt());
        }
    }

    private void insertPayload(
            long runId,
            String sourceName,
            String endpointName,
            String entityKey,
            String requestUri,
            Map<String, ?> requestParams,
            Object responseBody,
            boolean success,
            String errorMessage,
            Instant collectedAt) {
        jdbcTemplate.update("""
                        INSERT INTO api_payload_archive (
                            run_id, source_name, endpoint_name, entity_key, success, request_uri,
                            request_params_json, response_json, error_message, collected_at
                        ) VALUES (
                            :runId, :sourceName, :endpointName, :entityKey, :success, :requestUri,
                            :requestParamsJson, :responseJson, :errorMessage, :collectedAt
                        )
                        """,
                new MapSqlParameterSource()
                        .addValue("runId", runId)
                        .addValue("sourceName", sourceName)
                        .addValue("endpointName", endpointName)
                        .addValue("entityKey", entityKey)
                        .addValue("success", success)
                        .addValue("requestUri", requestUri)
                        .addValue("requestParamsJson", toJson(requestParams))
                        .addValue("responseJson", responseBody == null ? null : toJson(responseBody))
                        .addValue("errorMessage", errorMessage)
                        .addValue("collectedAt", toTimestamp(collectedAt)));
    }

    private String resolveHolderScanStatus(AlphaFuturesCandidateReport candidateReport) {
        if (!candidateReport.supportedChain()) {
            return "UNSUPPORTED_CHAIN";
        }
        if (candidateReport.topHolders().isEmpty()) {
            return "EMPTY";
        }
        if (candidateReport.rejectReasons().contains("holder_concentration_unavailable")) {
            return "UNAVAILABLE";
        }
        return "SUCCESS";
    }

    private Timestamp toTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value).longValueExact();
        } catch (ArithmeticException | NumberFormatException ex) {
            return null;
        }
    }

    private String trimMessage(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= 2000 ? value : value.substring(0, 2000);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize persistence payload.", ex);
        }
    }
}
