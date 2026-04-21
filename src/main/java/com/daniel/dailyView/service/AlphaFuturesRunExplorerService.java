package com.daniel.dailyView.service;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.daniel.dailyView.dto.AlphaFuturesRunCandidateView;
import com.daniel.dailyView.dto.AlphaFuturesRunDetailView;
import com.daniel.dailyView.dto.AlphaFuturesRunHolderView;
import com.daniel.dailyView.dto.AlphaFuturesRunPayloadView;
import com.daniel.dailyView.dto.AlphaFuturesRunStatsView;
import com.daniel.dailyView.dto.AlphaFuturesRunView;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;

@Service
public class AlphaFuturesRunExplorerService {

    private static final int DEFAULT_LIMIT = 12;
    private static final int MAX_LIMIT = 72;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AlphaFuturesRunExplorerService(
            NamedParameterJdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<AlphaFuturesRunView> listRecentRuns(Integer limit) {
        int effectiveLimit = normalizeLimit(limit);
        String sql = """
                SELECT id, job_name, trigger_type, status, started_at, ended_at, duration_ms, rules_json,
                       total_alpha_tokens, mapped_candidates, passed_candidates, rejected_candidates, error_message
                FROM screen_run
                ORDER BY id DESC
                LIMIT %d
                """.formatted(effectiveLimit);
        return jdbcTemplate.query(sql, new MapSqlParameterSource(), (rs, rowNum) -> mapRun(rs));
    }

    public AlphaFuturesRunDetailView getRunDetail(long runId) {
        AlphaFuturesRunView run = findRun(runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Run not found: " + runId));

        return new AlphaFuturesRunDetailView(
                run,
                loadStats(runId),
                loadCandidates(runId),
                loadTopHolders(runId),
                loadPayloads(runId)
        );
    }

    private Optional<AlphaFuturesRunView> findRun(long runId) {
        String sql = """
                SELECT id, job_name, trigger_type, status, started_at, ended_at, duration_ms, rules_json,
                       total_alpha_tokens, mapped_candidates, passed_candidates, rejected_candidates, error_message
                FROM screen_run
                WHERE id = :runId
                """;

        List<AlphaFuturesRunView> runs = jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("runId", runId),
                (rs, rowNum) -> mapRun(rs)
        );
        return runs.stream().findFirst();
    }

    private AlphaFuturesRunStatsView loadStats(long runId) {
        MapSqlParameterSource params = new MapSqlParameterSource("runId", runId);
        return new AlphaFuturesRunStatsView(
                count("SELECT COUNT(*) FROM alpha_token_snapshot WHERE run_id = :runId", params),
                count("SELECT COUNT(*) FROM futures_contract_snapshot WHERE run_id = :runId", params),
                count("SELECT COUNT(*) FROM screen_result WHERE run_id = :runId", params),
                count("SELECT COUNT(*) FROM api_payload_archive WHERE run_id = :runId", params),
                count("SELECT COUNT(*) FROM holder_detail_topn WHERE run_id = :runId", params),
                count("SELECT COUNT(*) FROM open_interest_history WHERE last_seen_run_id = :runId", params),
                count("SELECT COUNT(*) FROM price_kline WHERE last_seen_run_id = :runId", params)
        );
    }

    private List<AlphaFuturesRunCandidateView> loadCandidates(long runId) {
        String sql = """
                SELECT alpha_id, alpha_symbol, cex_coin_name, futures_symbol, chain_id, contract_address, listing_time,
                       current_open_interest_usdt, monthly_pump_pct, top10_holding_ratio_pct,
                       recent_week_average_oi_ratio, recent_week_peak_oi_ratio, top_holder_amount, supply_amount,
                       supply_consistent, supply_denominator_source, passed, reject_reasons_json
                FROM screen_result
                WHERE run_id = :runId
                ORDER BY passed DESC, current_open_interest_usdt DESC, futures_symbol ASC
                """;

        return jdbcTemplate.query(sql, new MapSqlParameterSource("runId", runId), (rs, rowNum) ->
                new AlphaFuturesRunCandidateView(
                        rs.getString("alpha_id"),
                        rs.getString("alpha_symbol"),
                        rs.getString("cex_coin_name"),
                        rs.getString("futures_symbol"),
                        rs.getString("chain_id"),
                        rs.getString("contract_address"),
                        toInstant(rs.getTimestamp("listing_time")),
                        rs.getBigDecimal("current_open_interest_usdt"),
                        rs.getBigDecimal("monthly_pump_pct"),
                        rs.getBigDecimal("top10_holding_ratio_pct"),
                        rs.getBigDecimal("recent_week_average_oi_ratio"),
                        rs.getBigDecimal("recent_week_peak_oi_ratio"),
                        rs.getBigDecimal("top_holder_amount"),
                        rs.getBigDecimal("supply_amount"),
                        rs.getBoolean("supply_consistent"),
                        rs.getString("supply_denominator_source"),
                        rs.getBoolean("passed"),
                        parseStringList(rs.getString("reject_reasons_json"))
                ));
    }

    private List<AlphaFuturesRunHolderView> loadTopHolders(long runId) {
        String sql = """
                SELECT d.alpha_id,
                       COALESCE(r.alpha_symbol, d.alpha_id) AS alpha_symbol,
                       COALESCE(r.futures_symbol, '') AS futures_symbol,
                       d.chain_id,
                       d.contract_address,
                       d.top_n,
                       d.rank_no,
                       d.wallet_address,
                       d.amount
                FROM holder_detail_topn d
                LEFT JOIN screen_result r
                       ON r.run_id = d.run_id
                      AND r.alpha_id = d.alpha_id
                WHERE d.run_id = :runId
                ORDER BY COALESCE(r.futures_symbol, d.alpha_id), d.rank_no
                """;

        return jdbcTemplate.query(sql, new MapSqlParameterSource("runId", runId), (rs, rowNum) ->
                new AlphaFuturesRunHolderView(
                        rs.getString("alpha_id"),
                        rs.getString("alpha_symbol"),
                        rs.getString("futures_symbol"),
                        rs.getString("chain_id"),
                        rs.getString("contract_address"),
                        rs.getInt("top_n"),
                        rs.getInt("rank_no"),
                        rs.getString("wallet_address"),
                        rs.getBigDecimal("amount")
                ));
    }

    private List<AlphaFuturesRunPayloadView> loadPayloads(long runId) {
        String sql = """
                SELECT source_name, endpoint_name, entity_key, success, request_uri,
                       request_params_json, response_json, error_message, collected_at
                FROM api_payload_archive
                WHERE run_id = :runId
                ORDER BY collected_at DESC, source_name ASC, endpoint_name ASC, entity_key ASC
                """;

        return jdbcTemplate.query(sql, new MapSqlParameterSource("runId", runId), (rs, rowNum) ->
                new AlphaFuturesRunPayloadView(
                        rs.getString("source_name"),
                        rs.getString("endpoint_name"),
                        rs.getString("entity_key"),
                        rs.getBoolean("success"),
                        rs.getString("request_uri"),
                        parseJsonNode(rs.getString("request_params_json")),
                        parseJsonNode(rs.getString("response_json")),
                        rs.getString("error_message"),
                        toInstant(rs.getTimestamp("collected_at"))
                ));
    }

    private AlphaFuturesRunView mapRun(ResultSet rs) throws SQLException {
        return new AlphaFuturesRunView(
                rs.getLong("id"),
                rs.getString("job_name"),
                rs.getString("trigger_type"),
                rs.getString("status"),
                toInstant(rs.getTimestamp("started_at")),
                toInstant(rs.getTimestamp("ended_at")),
                (Integer) rs.getObject("duration_ms"),
                parseJsonNode(rs.getString("rules_json")),
                (Integer) rs.getObject("total_alpha_tokens"),
                (Integer) rs.getObject("mapped_candidates"),
                (Integer) rs.getObject("passed_candidates"),
                (Integer) rs.getObject("rejected_candidates"),
                rs.getString("error_message")
        );
    }

    private int count(String sql, MapSqlParameterSource params) {
        Integer value = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return value == null ? 0 : value;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private JsonNode parseJsonNode(String value) {
        if (value == null || value.isBlank()) {
            return NullNode.instance;
        }
        try {
            return objectMapper.readTree(value);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse persisted JSON payload.", ex);
        }
    }

    private List<String> parseStringList(String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(value, new TypeReference<List<String>>() {
            });
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse persisted reject reasons JSON.", ex);
        }
    }
}
