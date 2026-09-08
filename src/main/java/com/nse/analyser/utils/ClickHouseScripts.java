package com.nse.analyser.utils;

public class ClickHouseScripts {
    public static final String CREATE_DB = """
                CREATE DATABASE IF NOT EXISTS option_chain
                """;

    public static final String CREATE_INGESTION_ID_TABLE = """
                    CREATE TABLE IF NOT EXISTS option_chain.ingestion_keys
                    (
                        id FixedString(64),
                        created_at DateTime DEFAULT now()
                    )
                    ENGINE = MergeTree
                    ORDER BY id;
                    """;

    public static final String CREATE_1M_TABLE= """
                CREATE TABLE IF NOT EXISTS option_chain.nse_snapshot_1m
                (
                    underlying String,
                    underlying_value Float64,

                    strike_price Float64,

                    option_type LowCardinality(String),
                    rank LowCardinality(String),
                    expiry_date Date,

                    open_interest UInt64,
                    change_in_open_interest Int64,

                    ltp Float64,
                    ltp_change Float64,

                    bid_quantity UInt64,
                    ask_quantity UInt64,
                    oi_concentration Float64,
                    order_book_imbalance Float64,
                    traded_volume UInt64,
                    volume_concentration Float64,
                    implied_volatility Float64,
                    activity LowCardinality(String),
                    nse_timestamp DateTime64(3, 'Asia/Kolkata'),

                    snapshot_timestamp DateTime64(3, 'Asia/Kolkata'),
                    id FixedString(64)
                )
                ENGINE = MergeTree
                PARTITION BY toYYYYMM(expiry_date)
                ORDER BY
                (
                    underlying,
                    expiry_date,
                    strike_price,
                    option_type,
                    nse_timestamp
                )
                """;

    public static final String AGG_TABLE_TEMPLATE = """
        CREATE TABLE IF NOT EXISTS option_chain.%s
        (
            underlying String,
            rank LowCardinality(String),

            strike_price Float64,
            option_type LowCardinality(String),
            expiry_date Date,

            bucket_start DateTime('Asia/Kolkata'),

            open_oi AggregateFunction(argMin, UInt64, DateTime64(3,'Asia/Kolkata')),
            high_oi AggregateFunction(max, UInt64),
            low_oi AggregateFunction(min, UInt64),
            close_oi AggregateFunction(argMax, UInt64, DateTime64(3,'Asia/Kolkata')),

            open_change_oi AggregateFunction(argMin, Int64, DateTime64(3,'Asia/Kolkata')),
            high_change_oi AggregateFunction(max, Int64),
            low_change_oi AggregateFunction(min, Int64),
            close_change_oi AggregateFunction(argMax, Int64, DateTime64(3,'Asia/Kolkata')),

            open_ltp AggregateFunction(argMin, Float64, DateTime64(3,'Asia/Kolkata')),
            high_ltp AggregateFunction(max, Float64),
            low_ltp AggregateFunction(min, Float64),
            close_ltp AggregateFunction(argMax, Float64, DateTime64(3,'Asia/Kolkata')),

            open_iv AggregateFunction(argMin, Float64, DateTime64(3,'Asia/Kolkata')),
            high_iv AggregateFunction(max, Float64),
            low_iv AggregateFunction(min, Float64),
            close_iv AggregateFunction(argMax, Float64, DateTime64(3,'Asia/Kolkata')),

            open_obi AggregateFunction(argMin, Float64, DateTime64(3,'Asia/Kolkata')),
            high_obi AggregateFunction(max, Float64),
            low_obi AggregateFunction(min, Float64),
            close_obi AggregateFunction(argMax, Float64, DateTime64(3,'Asia/Kolkata')),

            open_oi_concentration AggregateFunction(argMin, Float64, DateTime64(3,'Asia/Kolkata')),
            high_oi_concentration AggregateFunction(max, Float64),
            low_oi_concentration AggregateFunction(min, Float64),
            close_oi_concentration AggregateFunction(argMax, Float64, DateTime64(3,'Asia/Kolkata')),

            open_volume_concentration AggregateFunction(argMin, Float64, DateTime64(3,'Asia/Kolkata')),
            high_volume_concentration AggregateFunction(max, Float64),
            low_volume_concentration AggregateFunction(min, Float64),
            close_volume_concentration AggregateFunction(argMax, Float64, DateTime64(3,'Asia/Kolkata')),

            open_volume AggregateFunction(argMin, UInt64, DateTime64(3,'Asia/Kolkata')),
            high_volume AggregateFunction(max, UInt64),
            low_volume AggregateFunction(min, UInt64),
            close_volume AggregateFunction(argMax, UInt64, DateTime64(3,'Asia/Kolkata'))
        )
        ENGINE = AggregatingMergeTree
        PARTITION BY toYYYYMM(expiry_date)
        ORDER BY
        (
            underlying,
            rank,
            expiry_date,
            strike_price,
            option_type,
            bucket_start
        )
        """;

    public static final String MV_TEMPLATE = """
        CREATE MATERIALIZED VIEW IF NOT EXISTS option_chain.%s
        TO option_chain.%s
        AS
        SELECT
            underlying,
            rank,
            strike_price,
            option_type,
            expiry_date,

            toStartOfInterval(
                nse_timestamp,
                INTERVAL %s
            ) AS bucket_start,

            argMinState(open_interest, nse_timestamp) AS open_oi,
            maxState(open_interest) AS high_oi,
            minState(open_interest) AS low_oi,
            argMaxState(open_interest, nse_timestamp) AS close_oi,

            argMinState(change_in_open_interest, nse_timestamp) AS open_change_oi,
            maxState(change_in_open_interest) AS high_change_oi,
            minState(change_in_open_interest) AS low_change_oi,
            argMaxState(change_in_open_interest, nse_timestamp) AS close_change_oi,

            argMinState(ltp, nse_timestamp) AS open_ltp,
            maxState(ltp) AS high_ltp,
            minState(ltp) AS low_ltp,
            argMaxState(ltp, nse_timestamp) AS close_ltp,

            argMinState(implied_volatility, nse_timestamp) AS open_iv,
            maxState(implied_volatility) AS high_iv,
            minState(implied_volatility) AS low_iv,
            argMaxState(implied_volatility, nse_timestamp) AS close_iv,

            argMinState(order_book_imbalance, nse_timestamp) AS open_obi,
            maxState(order_book_imbalance) AS high_obi,
            minState(order_book_imbalance) AS low_obi,
            argMaxState(order_book_imbalance, nse_timestamp) AS close_obi,

            argMinState(oi_concentration, nse_timestamp) AS open_oi_concentration,
            maxState(oi_concentration) AS high_oi_concentration,
            minState(oi_concentration) AS low_oi_concentration,
            argMaxState(oi_concentration, nse_timestamp) AS close_oi_concentration,

            argMinState(volume_concentration, nse_timestamp) AS open_volume_concentration,
            maxState(volume_concentration) AS high_volume_concentration,
            minState(volume_concentration) AS low_volume_concentration,
            argMaxState(volume_concentration, nse_timestamp) AS close_volume_concentration,

            argMinState(traded_volume, nse_timestamp) AS open_volume,
            maxState(traded_volume) AS high_volume,
            minState(traded_volume) AS low_volume,
            argMaxState(traded_volume, nse_timestamp) AS close_volume

        FROM option_chain.nse_snapshot_1m

        GROUP BY
            underlying,
            rank,
            strike_price,
            option_type,
            expiry_date,
            bucket_start
        """;

    public static final String SNAPSHOT_TABLE_INSERT = """
                INSERT INTO option_chain.nse_snapshot_%s
                (
                    underlying,
                    underlying_value,
                    strike_price,
                    option_type,
                    rank,
                    expiry_date,
                    open_interest,
                    change_in_open_interest,
                    ltp,
                    ltp_change,
                    bid_quantity,
                    ask_quantity,
                    oi_concentration,
                    order_book_imbalance,
                    traded_volume,
                    volume_concentration,
                    implied_volatility,
                    activity,
                    nse_timestamp,
                    snapshot_timestamp,
                    id
                )
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """;

    public static final String CREATE_VIEW_FROM_AGG_MERGE_TREE = """
            CREATE VIEW option_chain.view_table_%s AS
            SELECT
            
                underlying,
                rank,
                strike_price,
                option_type,
                expiry_date,
                bucket_start,
            
                argMinMerge(open_oi) open_oi,
                maxMerge(high_oi) high_oi,
                minMerge(low_oi) low_oi,
                argMaxMerge(close_oi) close_oi,
            
                argMinMerge(open_ltp) open_ltp,
                maxMerge(high_ltp) high_ltp,
                minMerge(low_ltp) low_ltp,
                argMaxMerge(close_ltp) close_ltp,
            
                argMinMerge(open_iv) open_iv,
                maxMerge(high_iv) high_iv,
                minMerge(low_iv) low_iv,
                argMaxMerge(close_iv) close_iv,
            
                argMinMerge(open_volume) open_volume,
                maxMerge(high_volume) high_volume,
                minMerge(low_volume) low_volume,
                argMaxMerge(close_volume) close_volume
            
            FROM option_chain.nse_snapshot_%s
            GROUP BY
                underlying,
                rank,
                strike_price,
                option_type,
                expiry_date,
                bucket_start
            """;

    public static final String CREATE_YESTERDAY_COMP_TABLE = """
            CREATE VIEW option_chain.vw_day_over_day
            AS
            SELECT
            
                *,
            
                lag(close_oi,1) OVER w previous_oi,
                lag(close_ltp,1) OVER w previous_ltp,
                lag(close_iv,1) OVER w previous_iv,
                lag(close_volume,1) OVER w previous_volume
            
            FROM option_chain.view_table_1d
            
            WINDOW w AS
            (
                PARTITION BY
                    underlying,
                    expiry_date,
                    strike_price,
                    option_type
                ORDER BY bucket_start
            )
            """;
}
