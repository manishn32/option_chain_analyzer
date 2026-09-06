CREATE DATABASE IF NOT EXISTS option_chain;

CREATE TABLE IF NOT EXISTS option_chain.market_projection_1m
(
    timestamp DateTime,
    timeframe String,
    underlying String,
    expiry_date Date,
    spot_price Float64,

    support_strike Float64,
    support_strength Float64,

    resistance_strike Float64,
    resistance_strength Float64,

    bull_score Float64,
    bear_score Float64,
    smart_money_flow Float64,
    dealer_pressure Float64,
    breakout_probability Float64,
    strike_migration_score Float64,
    iv_bias Float64,
    market_projection_score Float64,
    regime String,
    commentary String
)
ENGINE = ReplacingMergeTree
ORDER BY (underlying, expiry_date, timestamp);

CREATE TABLE IF NOT EXISTS option_chain.market_projection_3m AS option_chain.market_projection_1m
ENGINE = ReplacingMergeTree
ORDER BY (underlying, expiry_date, timestamp);

CREATE TABLE IF NOT EXISTS option_chain.market_projection_5m AS option_chain.market_projection_1m
ENGINE = ReplacingMergeTree
ORDER BY (underlying, expiry_date, timestamp);

CREATE TABLE IF NOT EXISTS option_chain.market_projection_15m AS option_chain.market_projection_1m
ENGINE = ReplacingMergeTree
ORDER BY (underlying, expiry_date, timestamp);

CREATE TABLE IF NOT EXISTS option_chain.market_projection_30m AS option_chain.market_projection_1m
ENGINE = ReplacingMergeTree
ORDER BY (underlying, expiry_date, timestamp);

CREATE TABLE IF NOT EXISTS option_chain.market_projection_1h AS option_chain.market_projection_1m
ENGINE = ReplacingMergeTree
ORDER BY (underlying, expiry_date, timestamp);

CREATE TABLE IF NOT EXISTS option_chain.strike_migration
(
    timestamp DateTime,
    timeframe String,
    underlying String,
    expiry_date Date,
    support_strike Float64,
    resistance_strike Float64
)
ENGINE = ReplacingMergeTree
ORDER BY (underlying, expiry_date, timeframe, timestamp);

CREATE TABLE IF NOT EXISTS option_chain.projection_accuracy
(
    prediction_time DateTime,
    timeframe String,
    underlying String,
    expiry_date Date,
    projected_regime String,
    projected_score Float64,
    actual_move Float64,
    success UInt8
)
ENGINE = MergeTree
ORDER BY (underlying, expiry_date, timeframe, prediction_time);

