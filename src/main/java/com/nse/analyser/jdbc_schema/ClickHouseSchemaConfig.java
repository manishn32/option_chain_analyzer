package com.nse.analyser.jdbc_schema;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@RequiredArgsConstructor
public class ClickHouseSchemaConfig {

    private final JdbcTemplate jdbcTemplate;

    @Bean
    ApplicationRunner createSchema() {
        return args -> {

            jdbcTemplate.execute("""
                CREATE DATABASE IF NOT EXISTS option_chain
                """);

            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS option_chain.nse_snapshot_1s
                (
                    underlying String,
                    underlying_value Float64,

                    strike_price Float64,

                    option_type LowCardinality(String),

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

                    snapshot_timestamp DateTime64(3, 'UTC')
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
                """);
        };
    }
}