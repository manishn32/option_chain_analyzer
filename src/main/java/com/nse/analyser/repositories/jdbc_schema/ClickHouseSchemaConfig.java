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
            jdbcTemplate.execute(String.format(CREATE_TABLE,"1m"));
            jdbcTemplate.execute(String.format(CREATE_TABLE,"1D"));
        };
    }


        @Bean
        @Order(2)
        ApplicationRunner createAllMVs() {

            return args -> {

                createTimeframe(
                        "nse_snapshot_3m",
                        "mv_nse_snapshot_3m",
                        "3 MINUTE",
                        "nse_view_3m");

                createTimeframe(
                        "nse_snapshot_5m",
                        "mv_nse_snapshot_5m",
                        "5 MINUTE",
                        "nse_view_5m");

                createTimeframe(
                        "nse_snapshot_15m",
                        "mv_nse_snapshot_15m",
                        "15 MINUTE",
                        "nse_view_15m");

                createTimeframe(
                        "nse_snapshot_75m",
                        "mv_nse_snapshot_75m",
                        "75 MINUTE",
                        "nse_view_75m");

                createTimeframe(
                        "nse_snapshot_125m",
                        "mv_nse_snapshot_125m",
                        "125 MINUTE",
                        "nse_view_125m");

                createTimeframe(
                        "nse_snapshot_1D",
                        "mv_nse_snapshot_1D",
                        "1 DAY",
                        "nse_view_1D");

                createTimeframe(
                        "nse_snapshot_1W",
                        "mv_nse_snapshot_1W",
                        "1 WEEK",
                        "nse_view_1W");

//                createTimeframe(
//                        "nse_snapshot_1M",
//                        "mv_nse_snapshot_1M",
//                        "1 MONTH",
//                        "nse_view_1M");
//                monthlyMVCreation("mv_nse_snapshot_1M", "nse_snapshot_1M");
            };
        }

    private void monthlyMVCreation(String mvName, String tableName) {
        jdbcTemplate.execute(
                AGG_TABLE_TEMPLATE.formatted(tableName));
        jdbcTemplate.execute(
                MV_TEMPLATE_MONTHLY.formatted(
                        mvName,
                        tableName));
    }

    private void createTimeframe(
                String tableName,
                String mvName,
                String interval,
                String viewName) {

            jdbcTemplate.execute(
                    AGG_TABLE_TEMPLATE.formatted(tableName));

            jdbcTemplate.execute(
                    MV_TEMPLATE.formatted(
                            mvName,
                            tableName,
                            interval));

            jdbcTemplate.execute(CREATE_VIEW_FROM_AGG_MERGE_TREE.formatted(
                    viewName,
                    tableName));
        }
    }