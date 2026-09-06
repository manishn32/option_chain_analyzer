package com.nse.analyser.repositories.impl;

import com.nse.analyser.enums.OptionActivity;
import com.nse.analyser.enums.OptionType;
import com.nse.analyser.models.MarketProjection;
import com.nse.analyser.models.chainSnapShot.OptionLeg;
import com.nse.analyser.repositories.NseSnapshotRepository;
import com.nse.analyser.utils.ClickHouseScripts;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class NseSnapshotRepositoryImpl implements NseSnapshotRepository {

        private final JdbcTemplate jdbcTemplate;

        private final RowMapper<OptionLeg> ROW_MAPPER =
                (rs, rowNum) -> OptionLeg.builder()
                        .underlying(rs.getString("underlying"))
                        .underlyingValue(rs.getDouble("underlying_value"))
                        .strikePrice(rs.getDouble("strike_price"))
                        .optionType(OptionType.valueOf(rs.getString("option_type")))
                        .expiryDate(rs.getDate("expiry_date").toLocalDate())
                        .openInterest(rs.getLong("open_interest"))
                        .changeInOpenInterest(rs.getLong("change_in_open_interest"))
                        .LTP(rs.getDouble("ltp"))
                        .priceChange(rs.getDouble("ltp_change"))
                        .bidQuantity(rs.getLong("bid_quantity"))
                        .askQuantity(rs.getLong("ask_quantity"))
                        .oiConcentration(rs.getDouble("oi_concentration"))
                        .obi(rs.getDouble("order_book_imbalance"))
                        .totalTradedVolume(rs.getLong("traded_volume"))
                        .volumeConcentration(rs.getDouble("volume_concentration"))
                        .impliedVolatility(rs.getDouble("implied_volatility"))
                        .optionActivity(OptionActivity.valueOf(rs.getString("activity")))
                        .nseTimestamp(
                                rs.getTimestamp("nse_timestamp")
                                        .toLocalDateTime()
                        )
//                        .snapshotTimestamp(
//                                Instant.from(rs.getTimestamp("snapshot_timestamp")
//                                        .toLocalDateTime()))
                        .snapshotTimestamp(
                                rs.getTimestamp("snapshot_timestamp")
                                        .toInstant()
                        )
                        .build();

        @Override
        public void save(OptionLeg s) {

            jdbcTemplate.update("""
                INSERT INTO option_chain.nse_snapshot_1s
                (
                    underlying,
                    underlying_value,
                    strike_price,
                    option_type,
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
                    snapshot_timestamp
                )
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,
                    s.getUnderlying(),
                    s.getUnderlyingValue(),
                    s.getStrikePrice(),
                    s.getOptionType(),
                    s.getExpiryDate(),
                    s.getOpenInterest(),
                    s.getChangeInOpenInterest(),
                    s.getLTP(),
                    s.getPriceChange(),
                    s.getBidQuantity(),
                    s.getAskQuantity(),
                    s.getOiConcentration(),
                    s.getObi(),
                    s.getTotalTradedVolume(),
                    s.getVolumeConcentration(),
                    s.getImpliedVolatility(),
                    s.getOptionActivity(),
                    Timestamp.valueOf(s.getNseTimestamp()),
                    Timestamp.valueOf(String.valueOf(s.getSnapshotTimestamp()))
            );
        }

        @Override
        public void saveAll(List<OptionLeg> snapshots, String table) {

            jdbcTemplate.batchUpdate(String.format(ClickHouseScripts.SNAPSHOT_TABLE_INSERT,table),
                    snapshots,
                    snapshots.size(),
                    (ps, s) -> {
                        ps.setString(1,s.getId());
                        ps.setString(2, s.getUnderlying());
                        ps.setDouble(3, s.getUnderlyingValue());
                        ps.setDouble(4, s.getStrikePrice());
                        ps.setString(5, s.getOptionType().name());
                        ps.setString(6, s.getOptionRank().name());
                        ps.setObject(7, s.getExpiryDate());

                        ps.setLong(8, s.getOpenInterest());
                        ps.setLong(9, s.getChangeInOpenInterest());

                        ps.setDouble(10, s.getLTP());
                        ps.setDouble(11, s.getPriceChange());

                        ps.setLong(12, s.getBidQuantity());
                        ps.setLong(13, s.getAskQuantity());

                        ps.setDouble(14, s.getOiConcentration());
                        ps.setDouble(15, s.getObi());

                        ps.setLong(16, s.getTotalTradedVolume());
                        ps.setDouble(17, s.getVolumeConcentration());

                        ps.setDouble(18, s.getImpliedVolatility());

                        ps.setString(19, s.getOptionActivity().name());

                        ps.setTimestamp(
                                20,
                                Timestamp.valueOf(s.getNseTimestamp())
                        );

                        ps.setTimestamp(
                                21,
                                Timestamp.from(s.getSnapshotTimestamp())
                        );
                    });
        }

        @Override
        public List<OptionLeg> findLatest(
                String underlying,
                LocalDate expiryDate,
                int limit) {

            return jdbcTemplate.query("""
                SELECT *
                FROM option_chain.nse_snapshot_1s
                WHERE underlying = ?
                  AND expiry_date = ?
                ORDER BY snapshot_timestamp DESC
                LIMIT ?
                """,
                    ROW_MAPPER,
                    underlying,
                    expiryDate,
                    limit
            );
        }

        @Override
        public List<OptionLeg> findByStrike(
                String underlying,
                LocalDate expiryDate,
                Double strikePrice,
                String optionType) {

            return jdbcTemplate.query("""
                SELECT *
                FROM option_chain.nse_snapshot_1s
                WHERE underlying = ?
                  AND expiry_date = ?
                  AND strike_price = ?
                  AND option_type = ?
                ORDER BY snapshot_timestamp DESC
                """,
                    ROW_MAPPER,
                    underlying,
                    expiryDate,
                    strikePrice,
                    optionType
            );
        }

        @Override
        public void deleteOlderThan(LocalDateTime timestamp) {

            jdbcTemplate.update("""
                ALTER TABLE option_chain.nse_snapshot_1s
                DELETE WHERE snapshot_timestamp < ?
                """,
                    Timestamp.valueOf(timestamp)
            );
        }

    @Override
    public Set<String> findExistingIds(Collection<String> ids) {

        if (ids.isEmpty()) {
            return Collections.emptySet();
        }

        String placeholders =
                ids.stream()
                        .map(v -> "?")
                        .collect(Collectors.joining(","));

        String sql = """
        SELECT id
        FROM option_chain.ingestion_keys
        WHERE id IN (%s)
        """.formatted(placeholders);

        return new HashSet<>(
                jdbcTemplate.queryForList(
                        sql,
                        String.class,
                        ids.toArray()
                )
        );
    }

    @Override
    public void insertKeys(List<OptionLeg> snapshots) {

        jdbcTemplate.batchUpdate(
                """
                INSERT INTO option_chain.ingestion_keys(id)
                VALUES(?)
                """,
                snapshots,
                1000,
                (ps, snapshot) -> ps.setString(1, snapshot.getId())
        );
    }

    @Override
    public List<OptionLeg> findLatestForProjection(
            String tableName,
            String underlying,
            LocalDate expiryDate,
            int limit) {

        validateProjectionTableName(tableName);

        String sql = """
                SELECT *
                FROM option_chain.%s
                WHERE underlying = ?
                  AND expiry_date = ?
                ORDER BY snapshot_timestamp DESC
                LIMIT ?
                """.formatted(tableName);

        return jdbcTemplate.query(
                sql,
                ROW_MAPPER,
                underlying,
                expiryDate,
                limit
        );
    }

    @Override
    public void saveProjection(String tableName, MarketProjection projection) {

        validateProjectionTableName(tableName);

        String sql = """
                INSERT INTO option_chain.%s
                (
                    timestamp,
                    timeframe,
                    underlying,
                    expiry_date,
                    spot_price,
                    support_strike,
                    support_strength,
                    resistance_strike,
                    resistance_strength,
                    bull_score,
                    bear_score,
                    smart_money_flow,
                    dealer_pressure,
                    breakout_probability,
                    strike_migration_score,
                    iv_bias,
                    market_projection_score,
                    regime,
                    commentary
                )
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """.formatted(tableName);

        jdbcTemplate.update(
                sql,
                Timestamp.valueOf(projection.getTimestamp()),
                projection.getTimeframe(),
                projection.getUnderlying(),
                projection.getExpiryDate(),
                projection.getSpotPrice(),
                projection.getSupportStrike(),
                projection.getSupportStrength(),
                projection.getResistanceStrike(),
                projection.getResistanceStrength(),
                projection.getBullScore(),
                projection.getBearScore(),
                projection.getSmartMoneyFlow(),
                projection.getDealerPressure(),
                projection.getBreakoutProbability(),
                projection.getStrikeMigrationScore(),
                projection.getIvBias(),
                projection.getMarketProjectionScore(),
                projection.getMarketRegime(),
                projection.getCommentary()
        );
    }

    @Override
    public MarketProjection findLatestProjection(
            String tableName,
            String underlying,
            LocalDate expiryDate) {

        validateProjectionTableName(tableName);

        String sql = """
                SELECT *
                FROM option_chain.%s
                WHERE underlying = ?
                  AND expiry_date = ?
                ORDER BY timestamp DESC
                LIMIT 1
                """.formatted(tableName);

        List<MarketProjection> rows = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> MarketProjection.builder()
                        .timestamp(rs.getTimestamp("timestamp").toLocalDateTime())
                        .timeframe(rs.getString("timeframe"))
                        .underlying(rs.getString("underlying"))
                        .expiryDate(rs.getDate("expiry_date").toLocalDate())
                        .spotPrice(rs.getDouble("spot_price"))
                        .supportStrike(rs.getDouble("support_strike"))
                        .supportStrength(rs.getDouble("support_strength"))
                        .resistanceStrike(rs.getDouble("resistance_strike"))
                        .resistanceStrength(rs.getDouble("resistance_strength"))
                        .bullScore(rs.getDouble("bull_score"))
                        .bearScore(rs.getDouble("bear_score"))
                        .smartMoneyFlow(rs.getDouble("smart_money_flow"))
                        .dealerPressure(rs.getDouble("dealer_pressure"))
                        .breakoutProbability(rs.getDouble("breakout_probability"))
                        .strikeMigrationScore(rs.getDouble("strike_migration_score"))
                        .ivBias(rs.getDouble("iv_bias"))
                        .marketProjectionScore(rs.getDouble("market_projection_score"))
                        .marketRegime(rs.getString("regime"))
                        .commentary(rs.getString("commentary"))
                        .build(),
                underlying,
                expiryDate
        );

        return rows.isEmpty() ? null : rows.getFirst();
    }

    @Override
    public void saveStrikeMigration(
            String timeframe,
            String underlying,
            LocalDate expiryDate,
            double supportStrike,
            double resistanceStrike) {

        jdbcTemplate.update(
                """
                INSERT INTO option_chain.strike_migration
                (
                    timestamp,
                    timeframe,
                    underlying,
                    expiry_date,
                    support_strike,
                    resistance_strike
                )
                VALUES (?,?,?,?,?,?)
                """,
                Timestamp.valueOf(LocalDateTime.now()),
                timeframe,
                underlying,
                expiryDate,
                supportStrike,
                resistanceStrike
        );
    }

    @Override
    public void saveProjectionAccuracy(
            String timeframe,
            String underlying,
            LocalDate expiryDate,
            String projectedRegime,
            double projectedScore,
            double actualMove,
            boolean success) {

        jdbcTemplate.update(
                """
                INSERT INTO option_chain.projection_accuracy
                (
                    prediction_time,
                    timeframe,
                    underlying,
                    expiry_date,
                    projected_regime,
                    projected_score,
                    actual_move,
                    success
                )
                VALUES (?,?,?,?,?,?,?,?)
                """,
                Timestamp.valueOf(LocalDateTime.now()),
                timeframe,
                underlying,
                expiryDate,
                projectedRegime,
                projectedScore,
                actualMove,
                success ? 1 : 0
        );
    }

    @Override
    public List<MarketProjection> findRecentProjections(
            String tableName,
            String underlying,
            LocalDate expiryDate,
            int limit) {

        validateProjectionTableName(tableName);

        String sql = """
                SELECT *
                FROM option_chain.%s
                WHERE underlying = ?
                  AND expiry_date = ?
                ORDER BY timestamp DESC
                LIMIT ?
                """.formatted(tableName);

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> MarketProjection.builder()
                        .timestamp(rs.getTimestamp("timestamp").toLocalDateTime())
                        .timeframe(rs.getString("timeframe"))
                        .underlying(rs.getString("underlying"))
                        .expiryDate(rs.getDate("expiry_date").toLocalDate())
                        .spotPrice(rs.getDouble("spot_price"))
                        .supportStrike(rs.getDouble("support_strike"))
                        .supportStrength(rs.getDouble("support_strength"))
                        .resistanceStrike(rs.getDouble("resistance_strike"))
                        .resistanceStrength(rs.getDouble("resistance_strength"))
                        .bullScore(rs.getDouble("bull_score"))
                        .bearScore(rs.getDouble("bear_score"))
                        .smartMoneyFlow(rs.getDouble("smart_money_flow"))
                        .dealerPressure(rs.getDouble("dealer_pressure"))
                        .breakoutProbability(rs.getDouble("breakout_probability"))
                        .strikeMigrationScore(rs.getDouble("strike_migration_score"))
                        .ivBias(rs.getDouble("iv_bias"))
                        .marketProjectionScore(rs.getDouble("market_projection_score"))
                        .marketRegime(rs.getString("regime"))
                        .commentary(rs.getString("commentary"))
                        .build(),
                underlying,
                expiryDate,
                limit
        );
    }

    private void validateProjectionTableName(String tableName) {
        Set<String> allowed = Set.of(
                "nse_snapshot_1m",
                "nse_snapshot_3m",
                "nse_snapshot_5m",
                "nse_snapshot_15m",
                "nse_snapshot_30m",
                "nse_snapshot_1h",
                "market_projection_1m",
                "market_projection_3m",
                "market_projection_5m",
                "market_projection_15m",
                "market_projection_30m",
                "market_projection_1h"
        );

        if (!allowed.contains(tableName)) {
            throw new IllegalArgumentException("Invalid table name: " + tableName);
        }
    }
}
