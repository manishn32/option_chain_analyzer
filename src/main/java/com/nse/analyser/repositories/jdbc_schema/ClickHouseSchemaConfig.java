package com.nse.analyser.repositories.jdbc_schema;

import com.nse.analyser.utils.ClickHouseScripts;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static com.nse.analyser.utils.ClickHouseScripts.*;

@Configuration
@RequiredArgsConstructor
public class ClickHouseSchemaConfig {

    private final JdbcTemplate jdbcTemplate;


    @Bean
    @Order(1)
    ApplicationRunner createSchema() {
        return args -> {

            jdbcTemplate.execute(CREATE_DB);
            jdbcTemplate.execute(CREATE_INGESTION_ID_TABLE);
            jdbcTemplate.execute(CREATE_1M_TABLE);
        };
    }


        @Bean
        @Order(2)
        ApplicationRunner createAllMVs() {

            return args -> {

                createTimeframe(
                        "nse_snapshot_3m",
                        "mv_nse_snapshot_3m",
                        "3 MINUTE");

                createTimeframe(
                        "nse_snapshot_5m",
                        "mv_nse_snapshot_5m",
                        "5 MINUTE");

                createTimeframe(
                        "nse_snapshot_15m",
                        "mv_nse_snapshot_15m",
                        "15 MINUTE");

                createTimeframe(
                        "nse_snapshot_75m",
                        "mv_nse_snapshot_75m",
                        "75 MINUTE");

                createTimeframe(
                        "nse_snapshot_125m",
                        "mv_nse_snapshot_125m",
                        "125 MINUTE");

                createTimeframe(
                        "nse_snapshot_1D",
                        "mv_nse_snapshot_1D",
                        "1 DAY");

                createTimeframe(
                        "nse_snapshot_1W",
                        "mv_nse_snapshot_1W",
                        "1 WEEK");

                createTimeframe(
                        "nse_snapshot_1M",
                        "mv_nse_snapshot_1M",
                        "1 MONTH");
            };
        }

        private void createTimeframe(
                String tableName,
                String mvName,
                String interval) {

            jdbcTemplate.execute(
                    AGG_TABLE_TEMPLATE.formatted(tableName));

            jdbcTemplate.execute(
                    MV_TEMPLATE.formatted(
                            mvName,
                            tableName,
                            interval));
        }
    }