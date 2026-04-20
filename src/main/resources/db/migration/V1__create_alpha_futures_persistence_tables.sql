CREATE TABLE IF NOT EXISTS screen_run (
    id BIGINT NOT NULL AUTO_INCREMENT,
    job_name VARCHAR(64) NOT NULL,
    trigger_type VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    started_at DATETIME(3) NOT NULL,
    ended_at DATETIME(3) NULL,
    duration_ms INT NULL,
    rules_json JSON NULL,
    total_alpha_tokens INT NULL,
    mapped_candidates INT NULL,
    passed_candidates INT NULL,
    rejected_candidates INT NULL,
    error_message TEXT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_run_job_started (job_name, started_at DESC),
    KEY idx_run_status_started (status, started_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS screen_result (
    id BIGINT NOT NULL AUTO_INCREMENT,
    run_id BIGINT NOT NULL,
    alpha_id VARCHAR(32) NOT NULL,
    alpha_symbol VARCHAR(64) NULL,
    cex_coin_name VARCHAR(64) NULL,
    futures_symbol VARCHAR(64) NOT NULL,
    chain_id VARCHAR(32) NULL,
    contract_address VARCHAR(128) NULL,
    listing_time DATETIME(3) NULL,
    current_open_interest_usdt DECIMAL(36,8) NULL,
    monthly_pump_pct DECIMAL(18,4) NULL,
    top10_holding_ratio_pct DECIMAL(18,4) NULL,
    recent_week_average_oi_ratio DECIMAL(18,6) NULL,
    recent_week_peak_oi_ratio DECIMAL(18,6) NULL,
    top_holder_amount DECIMAL(38,18) NULL,
    supply_amount DECIMAL(38,18) NULL,
    supply_consistent TINYINT(1) NOT NULL DEFAULT 0,
    supply_denominator_source VARCHAR(32) NULL,
    passed TINYINT(1) NOT NULL DEFAULT 0,
    reject_reasons_json JSON NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_result_run_symbol (run_id, futures_symbol),
    KEY idx_result_run_passed (run_id, passed),
    KEY idx_result_symbol (futures_symbol),
    KEY idx_result_alpha (alpha_id),
    CONSTRAINT fk_screen_result_run
        FOREIGN KEY (run_id) REFERENCES screen_run (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS screen_result_current (
    id BIGINT NOT NULL AUTO_INCREMENT,
    run_id BIGINT NOT NULL,
    alpha_id VARCHAR(32) NOT NULL,
    alpha_symbol VARCHAR(64) NULL,
    cex_coin_name VARCHAR(64) NULL,
    futures_symbol VARCHAR(64) NOT NULL,
    chain_id VARCHAR(32) NULL,
    contract_address VARCHAR(128) NULL,
    listing_time DATETIME(3) NULL,
    current_open_interest_usdt DECIMAL(36,8) NULL,
    monthly_pump_pct DECIMAL(18,4) NULL,
    top10_holding_ratio_pct DECIMAL(18,4) NULL,
    recent_week_average_oi_ratio DECIMAL(18,6) NULL,
    recent_week_peak_oi_ratio DECIMAL(18,6) NULL,
    top_holder_amount DECIMAL(38,18) NULL,
    supply_amount DECIMAL(38,18) NULL,
    supply_consistent TINYINT(1) NOT NULL DEFAULT 0,
    supply_denominator_source VARCHAR(32) NULL,
    passed TINYINT(1) NOT NULL DEFAULT 0,
    reject_reasons_json JSON NULL,
    last_refreshed_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_current_futures_symbol (futures_symbol),
    KEY idx_current_passed (passed, last_refreshed_at DESC),
    KEY idx_current_alpha (alpha_id),
    CONSTRAINT fk_screen_result_current_run
        FOREIGN KEY (run_id) REFERENCES screen_run (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS alpha_token_snapshot (
    id BIGINT NOT NULL AUTO_INCREMENT,
    run_id BIGINT NOT NULL,
    alpha_id VARCHAR(32) NOT NULL,
    symbol VARCHAR(64) NULL,
    cex_coin_name VARCHAR(64) NULL,
    chain_id VARCHAR(32) NULL,
    contract_address VARCHAR(128) NULL,
    listing_time DATETIME(3) NULL,
    total_supply DECIMAL(38,18) NULL,
    circulating_supply DECIMAL(38,18) NULL,
    holders BIGINT NULL,
    fully_delisted TINYINT(1) NOT NULL DEFAULT 0,
    offsell TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_alpha_snapshot_run_alpha (run_id, alpha_id),
    KEY idx_alpha_snapshot_alpha (alpha_id),
    KEY idx_alpha_snapshot_cex (cex_coin_name),
    KEY idx_alpha_snapshot_contract (chain_id, contract_address),
    CONSTRAINT fk_alpha_snapshot_run
        FOREIGN KEY (run_id) REFERENCES screen_run (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS futures_contract_snapshot (
    id BIGINT NOT NULL AUTO_INCREMENT,
    run_id BIGINT NOT NULL,
    symbol VARCHAR(64) NOT NULL,
    base_asset VARCHAR(64) NULL,
    quote_asset VARCHAR(32) NULL,
    contract_type VARCHAR(32) NULL,
    status VARCHAR(32) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_futures_snapshot_run_symbol (run_id, symbol),
    KEY idx_futures_symbol (symbol),
    KEY idx_futures_base_quote (base_asset, quote_asset, status),
    CONSTRAINT fk_futures_snapshot_run
        FOREIGN KEY (run_id) REFERENCES screen_run (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS open_interest_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    symbol VARCHAR(64) NOT NULL,
    period_code VARCHAR(8) NOT NULL,
    source_timestamp DATETIME(3) NOT NULL,
    open_interest_value_usdt DECIMAL(36,8) NULL,
    first_seen_run_id BIGINT NULL,
    last_seen_run_id BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_oi_symbol_period_ts (symbol, period_code, source_timestamp),
    KEY idx_oi_symbol_period_ts (symbol, period_code, source_timestamp DESC),
    KEY idx_oi_last_seen_run (last_seen_run_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS price_kline (
    id BIGINT NOT NULL AUTO_INCREMENT,
    symbol VARCHAR(64) NOT NULL,
    interval_code VARCHAR(8) NOT NULL,
    open_time DATETIME(3) NOT NULL,
    close_time DATETIME(3) NULL,
    open_price DECIMAL(36,18) NULL,
    high_price DECIMAL(36,18) NULL,
    low_price DECIMAL(36,18) NULL,
    close_price DECIMAL(36,18) NULL,
    volume DECIMAL(36,18) NULL,
    quote_volume DECIMAL(36,18) NULL,
    trade_count INT NULL,
    first_seen_run_id BIGINT NULL,
    last_seen_run_id BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_kline_symbol_interval_open (symbol, interval_code, open_time),
    KEY idx_kline_symbol_interval_open (symbol, interval_code, open_time DESC),
    KEY idx_kline_last_seen_run (last_seen_run_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS holder_concentration_snapshot (
    id BIGINT NOT NULL AUTO_INCREMENT,
    run_id BIGINT NOT NULL,
    alpha_id VARCHAR(32) NOT NULL,
    futures_symbol VARCHAR(64) NOT NULL,
    chain_id VARCHAR(32) NULL,
    contract_address VARCHAR(128) NULL,
    top_n INT NOT NULL,
    top_holder_amount DECIMAL(38,18) NULL,
    supply_amount DECIMAL(38,18) NULL,
    holding_ratio_pct DECIMAL(18,6) NULL,
    supply_consistent TINYINT(1) NOT NULL DEFAULT 0,
    denominator_source VARCHAR(32) NULL,
    supported_chain TINYINT(1) NOT NULL DEFAULT 0,
    scan_status VARCHAR(32) NULL,
    error_message TEXT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_holder_snapshot_run_contract_topn (run_id, chain_id, contract_address, top_n),
    KEY idx_holder_snapshot_alpha (alpha_id),
    KEY idx_holder_snapshot_symbol (futures_symbol),
    KEY idx_holder_snapshot_ratio (holding_ratio_pct DESC),
    CONSTRAINT fk_holder_snapshot_run
        FOREIGN KEY (run_id) REFERENCES screen_run (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS holder_detail_topn (
    id BIGINT NOT NULL AUTO_INCREMENT,
    run_id BIGINT NOT NULL,
    alpha_id VARCHAR(32) NOT NULL,
    chain_id VARCHAR(32) NULL,
    contract_address VARCHAR(128) NULL,
    top_n INT NOT NULL,
    rank_no INT NOT NULL,
    wallet_address VARCHAR(128) NOT NULL,
    amount DECIMAL(38,18) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_holder_detail_run_contract_rank (run_id, chain_id, contract_address, rank_no),
    KEY idx_holder_detail_contract (chain_id, contract_address),
    KEY idx_holder_detail_wallet (wallet_address),
    KEY idx_holder_detail_alpha (alpha_id),
    CONSTRAINT fk_holder_detail_run
        FOREIGN KEY (run_id) REFERENCES screen_run (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS api_payload_archive (
    id BIGINT NOT NULL AUTO_INCREMENT,
    run_id BIGINT NOT NULL,
    source_name VARCHAR(32) NOT NULL,
    endpoint_name VARCHAR(64) NOT NULL,
    entity_key VARCHAR(128) NOT NULL,
    success TINYINT(1) NOT NULL DEFAULT 1,
    request_uri VARCHAR(512) NULL,
    request_params_json JSON NULL,
    response_json JSON NULL,
    error_message TEXT NULL,
    collected_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payload_run_endpoint_entity (run_id, endpoint_name, entity_key),
    KEY idx_payload_source_time (source_name, collected_at DESC),
    KEY idx_payload_entity (entity_key),
    KEY idx_payload_success (success, collected_at DESC),
    CONSTRAINT fk_payload_run
        FOREIGN KEY (run_id) REFERENCES screen_run (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
