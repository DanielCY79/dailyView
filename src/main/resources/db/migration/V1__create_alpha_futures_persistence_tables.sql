CREATE TABLE IF NOT EXISTS screen_run (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    job_name VARCHAR(64) NOT NULL COMMENT '任务名称',
    trigger_type VARCHAR(16) NOT NULL COMMENT '触发方式',
    status VARCHAR(16) NOT NULL COMMENT '运行状态',
    started_at DATETIME(3) NOT NULL COMMENT '开始时间',
    ended_at DATETIME(3) NULL COMMENT '结束时间',
    duration_ms INT NULL COMMENT '执行耗时（毫秒）',
    rules_json JSON NULL COMMENT '筛选规则JSON',
    total_alpha_tokens INT NULL COMMENT 'Alpha 代币总数',
    mapped_candidates INT NULL COMMENT '映射后的候选数',
    passed_candidates INT NULL COMMENT '通过筛选的候选数',
    rejected_candidates INT NULL COMMENT '被拒绝的候选数',
    error_message TEXT NULL COMMENT '错误信息',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_run_job_started (job_name, started_at DESC),
    KEY idx_run_status_started (status, started_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='筛选任务运行记录表';

CREATE TABLE IF NOT EXISTS screen_result (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    run_id BIGINT NOT NULL COMMENT '关联的运行记录ID',
    alpha_id VARCHAR(32) NOT NULL COMMENT 'Alpha 平台代币ID',
    alpha_symbol VARCHAR(64) NULL COMMENT 'Alpha 平台代币符号',
    cex_coin_name VARCHAR(64) NULL COMMENT '中心化交易所币种名称',
    futures_symbol VARCHAR(64) NOT NULL COMMENT '合约交易对符号',
    chain_id VARCHAR(32) NULL COMMENT '链ID',
    contract_address VARCHAR(128) NULL COMMENT '合约地址',
    listing_time DATETIME(3) NULL COMMENT '上线时间',
    current_open_interest_usdt DECIMAL(36,8) NULL COMMENT '当前持仓量（USDT）',
    monthly_pump_pct DECIMAL(18,4) NULL COMMENT '近一个月涨幅百分比',
    top10_holding_ratio_pct DECIMAL(18,4) NULL COMMENT '前十大持仓占比百分比',
    recent_week_average_oi_ratio DECIMAL(18,6) NULL COMMENT '近一周平均持仓量比率',
    recent_week_peak_oi_ratio DECIMAL(18,6) NULL COMMENT '近一周峰值持仓量比率',
    top_holder_amount DECIMAL(38,18) NULL COMMENT '头部地址持仓数量',
    supply_amount DECIMAL(38,18) NULL COMMENT '供应量',
    supply_consistent TINYINT(1) NOT NULL DEFAULT 0 COMMENT '供应量是否一致',
    supply_denominator_source VARCHAR(32) NULL COMMENT '供应量分母来源',
    passed TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否通过筛选',
    reject_reasons_json JSON NULL COMMENT '拒绝原因JSON',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_result_run_symbol (run_id, futures_symbol),
    KEY idx_result_run_passed (run_id, passed),
    KEY idx_result_symbol (futures_symbol),
    KEY idx_result_alpha (alpha_id),
    CONSTRAINT fk_screen_result_run
        FOREIGN KEY (run_id) REFERENCES screen_run (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='筛选结果明细表';

CREATE TABLE IF NOT EXISTS screen_result_current (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    run_id BIGINT NOT NULL COMMENT '关联的运行记录ID',
    alpha_id VARCHAR(32) NOT NULL COMMENT 'Alpha 平台代币ID',
    alpha_symbol VARCHAR(64) NULL COMMENT 'Alpha 平台代币符号',
    cex_coin_name VARCHAR(64) NULL COMMENT '中心化交易所币种名称',
    futures_symbol VARCHAR(64) NOT NULL COMMENT '合约交易对符号',
    chain_id VARCHAR(32) NULL COMMENT '链ID',
    contract_address VARCHAR(128) NULL COMMENT '合约地址',
    listing_time DATETIME(3) NULL COMMENT '上线时间',
    current_open_interest_usdt DECIMAL(36,8) NULL COMMENT '当前持仓量（USDT）',
    monthly_pump_pct DECIMAL(18,4) NULL COMMENT '近一个月涨幅百分比',
    top10_holding_ratio_pct DECIMAL(18,4) NULL COMMENT '前十大持仓占比百分比',
    recent_week_average_oi_ratio DECIMAL(18,6) NULL COMMENT '近一周平均持仓量比率',
    recent_week_peak_oi_ratio DECIMAL(18,6) NULL COMMENT '近一周峰值持仓量比率',
    top_holder_amount DECIMAL(38,18) NULL COMMENT '头部地址持仓数量',
    supply_amount DECIMAL(38,18) NULL COMMENT '供应量',
    supply_consistent TINYINT(1) NOT NULL DEFAULT 0 COMMENT '供应量是否一致',
    supply_denominator_source VARCHAR(32) NULL COMMENT '供应量分母来源',
    passed TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否通过筛选',
    reject_reasons_json JSON NULL COMMENT '拒绝原因JSON',
    last_refreshed_at DATETIME(3) NOT NULL COMMENT '最近刷新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_current_futures_symbol (futures_symbol),
    KEY idx_current_passed (passed, last_refreshed_at DESC),
    KEY idx_current_alpha (alpha_id),
    CONSTRAINT fk_screen_result_current_run
        FOREIGN KEY (run_id) REFERENCES screen_run (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='当前筛选结果快照表';

CREATE TABLE IF NOT EXISTS alpha_token_snapshot (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    run_id BIGINT NOT NULL COMMENT '关联的运行记录ID',
    alpha_id VARCHAR(32) NOT NULL COMMENT 'Alpha 平台代币ID',
    symbol VARCHAR(64) NULL COMMENT '代币符号',
    cex_coin_name VARCHAR(64) NULL COMMENT '中心化交易所币种名称',
    chain_id VARCHAR(32) NULL COMMENT '链ID',
    contract_address VARCHAR(128) NULL COMMENT '合约地址',
    listing_time DATETIME(3) NULL COMMENT '上线时间',
    total_supply DECIMAL(38,18) NULL COMMENT '总供应量',
    circulating_supply DECIMAL(38,18) NULL COMMENT '流通供应量',
    holders BIGINT NULL COMMENT '持有人数量',
    fully_delisted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已完全下架',
    offsell TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否禁止卖出',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_alpha_snapshot_run_alpha (run_id, alpha_id),
    KEY idx_alpha_snapshot_alpha (alpha_id),
    KEY idx_alpha_snapshot_cex (cex_coin_name),
    KEY idx_alpha_snapshot_contract (chain_id, contract_address),
    CONSTRAINT fk_alpha_snapshot_run
        FOREIGN KEY (run_id) REFERENCES screen_run (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Alpha 代币快照表';

CREATE TABLE IF NOT EXISTS futures_contract_snapshot (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    run_id BIGINT NOT NULL COMMENT '关联的运行记录ID',
    symbol VARCHAR(64) NOT NULL COMMENT '合约交易对符号',
    base_asset VARCHAR(64) NULL COMMENT '基础资产',
    quote_asset VARCHAR(32) NULL COMMENT '计价资产',
    contract_type VARCHAR(32) NULL COMMENT '合约类型',
    status VARCHAR(32) NULL COMMENT '合约状态',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_futures_snapshot_run_symbol (run_id, symbol),
    KEY idx_futures_symbol (symbol),
    KEY idx_futures_base_quote (base_asset, quote_asset, status),
    CONSTRAINT fk_futures_snapshot_run
        FOREIGN KEY (run_id) REFERENCES screen_run (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='合约基础信息快照表';

CREATE TABLE IF NOT EXISTS open_interest_history (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    symbol VARCHAR(64) NOT NULL COMMENT '合约交易对符号',
    period_code VARCHAR(8) NOT NULL COMMENT '周期编码',
    source_timestamp DATETIME(3) NOT NULL COMMENT '源数据时间戳',
    open_interest_value_usdt DECIMAL(36,8) NULL COMMENT '持仓量数值（USDT）',
    first_seen_run_id BIGINT NULL COMMENT '首次发现的运行记录ID',
    last_seen_run_id BIGINT NULL COMMENT '最近发现的运行记录ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_oi_symbol_period_ts (symbol, period_code, source_timestamp),
    KEY idx_oi_symbol_period_ts (symbol, period_code, source_timestamp DESC),
    KEY idx_oi_last_seen_run (last_seen_run_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='持仓量历史表';

CREATE TABLE IF NOT EXISTS price_kline (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    symbol VARCHAR(64) NOT NULL COMMENT '合约交易对符号',
    interval_code VARCHAR(8) NOT NULL COMMENT 'K线周期编码',
    open_time DATETIME(3) NOT NULL COMMENT '开盘时间',
    close_time DATETIME(3) NULL COMMENT '收盘时间',
    open_price DECIMAL(36,18) NULL COMMENT '开盘价',
    high_price DECIMAL(36,18) NULL COMMENT '最高价',
    low_price DECIMAL(36,18) NULL COMMENT '最低价',
    close_price DECIMAL(36,18) NULL COMMENT '收盘价',
    volume DECIMAL(36,18) NULL COMMENT '成交量',
    quote_volume DECIMAL(36,18) NULL COMMENT '成交额',
    trade_count INT NULL COMMENT '成交笔数',
    first_seen_run_id BIGINT NULL COMMENT '首次发现的运行记录ID',
    last_seen_run_id BIGINT NULL COMMENT '最近发现的运行记录ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_kline_symbol_interval_open (symbol, interval_code, open_time),
    KEY idx_kline_symbol_interval_open (symbol, interval_code, open_time DESC),
    KEY idx_kline_last_seen_run (last_seen_run_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='价格K线历史表';

CREATE TABLE IF NOT EXISTS holder_concentration_snapshot (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    run_id BIGINT NOT NULL COMMENT '关联的运行记录ID',
    alpha_id VARCHAR(32) NOT NULL COMMENT 'Alpha 平台代币ID',
    futures_symbol VARCHAR(64) NOT NULL COMMENT '合约交易对符号',
    chain_id VARCHAR(32) NULL COMMENT '链ID',
    contract_address VARCHAR(128) NULL COMMENT '合约地址',
    top_n INT NOT NULL COMMENT '统计前N大持仓',
    top_holder_amount DECIMAL(38,18) NULL COMMENT '前N大持仓总量',
    supply_amount DECIMAL(38,18) NULL COMMENT '供应量',
    holding_ratio_pct DECIMAL(18,6) NULL COMMENT '持仓占比百分比',
    supply_consistent TINYINT(1) NOT NULL DEFAULT 0 COMMENT '供应量是否一致',
    denominator_source VARCHAR(32) NULL COMMENT '分母来源',
    supported_chain TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为支持的链',
    scan_status VARCHAR(32) NULL COMMENT '扫描状态',
    error_message TEXT NULL COMMENT '错误信息',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_holder_snapshot_run_contract_topn (run_id, chain_id, contract_address, top_n),
    KEY idx_holder_snapshot_alpha (alpha_id),
    KEY idx_holder_snapshot_symbol (futures_symbol),
    KEY idx_holder_snapshot_ratio (holding_ratio_pct DESC),
    CONSTRAINT fk_holder_snapshot_run
        FOREIGN KEY (run_id) REFERENCES screen_run (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='持仓集中度快照表';

CREATE TABLE IF NOT EXISTS holder_detail_topn (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    run_id BIGINT NOT NULL COMMENT '关联的运行记录ID',
    alpha_id VARCHAR(32) NOT NULL COMMENT 'Alpha 平台代币ID',
    chain_id VARCHAR(32) NULL COMMENT '链ID',
    contract_address VARCHAR(128) NULL COMMENT '合约地址',
    top_n INT NOT NULL COMMENT '所属前N大持仓统计',
    rank_no INT NOT NULL COMMENT '排名序号',
    wallet_address VARCHAR(128) NOT NULL COMMENT '钱包地址',
    amount DECIMAL(38,18) NULL COMMENT '持仓数量',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_holder_detail_run_contract_rank (run_id, chain_id, contract_address, rank_no),
    KEY idx_holder_detail_contract (chain_id, contract_address),
    KEY idx_holder_detail_wallet (wallet_address),
    KEY idx_holder_detail_alpha (alpha_id),
    CONSTRAINT fk_holder_detail_run
        FOREIGN KEY (run_id) REFERENCES screen_run (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='前N大持仓地址明细表';

CREATE TABLE IF NOT EXISTS api_payload_archive (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    run_id BIGINT NOT NULL COMMENT '关联的运行记录ID',
    source_name VARCHAR(32) NOT NULL COMMENT '数据来源名称',
    endpoint_name VARCHAR(64) NOT NULL COMMENT '接口名称',
    entity_key VARCHAR(128) NOT NULL COMMENT '业务实体键',
    success TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否请求成功',
    request_uri VARCHAR(512) NULL COMMENT '请求地址',
    request_params_json JSON NULL COMMENT '请求参数JSON',
    response_json JSON NULL COMMENT '响应报文JSON',
    error_message TEXT NULL COMMENT '错误信息',
    collected_at DATETIME(3) NOT NULL COMMENT '采集时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_payload_run_endpoint_entity (run_id, endpoint_name, entity_key),
    KEY idx_payload_source_time (source_name, collected_at DESC),
    KEY idx_payload_entity (entity_key),
    KEY idx_payload_success (success, collected_at DESC),
    CONSTRAINT fk_payload_run
        FOREIGN KEY (run_id) REFERENCES screen_run (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='接口原始报文归档表';
