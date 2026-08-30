package com.nse.analyser.repositories.impl;

import com.nse.analyser.enums.OptionActivity;
import com.nse.analyser.enums.OptionType;
import com.nse.analyser.models.chainSnapShot.OptionLeg;
import com.nse.analyser.repositories.NseSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
                        .snapshotTimestamp(
                                Instant.from(rs.getTimestamp("snapshot_timestamp")
                                        .toLocalDateTime()))
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
        public void saveAll(List<OptionLeg> snapshots) {

            jdbcTemplate.batchUpdate("""
                INSERT INTO option_chain.nse_snapshot_1m
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
                    snapshot_timestamp
                )
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,
                    snapshots,
                    snapshots.size(),
                    (ps, s) -> {

                        ps.setString(1, s.getUnderlying());
                        ps.setDouble(2, s.getUnderlyingValue());
                        ps.setDouble(3, s.getStrikePrice());
                        ps.setString(4, s.getOptionType().name());
                        ps.setString(5, s.getOptionRank().name());
                        ps.setObject(6, s.getExpiryDate());

                        ps.setLong(7, s.getOpenInterest());
                        ps.setLong(8, s.getChangeInOpenInterest());

                        ps.setDouble(9, s.getLTP());
                        ps.setDouble(10, s.getPriceChange());

                        ps.setLong(11, s.getBidQuantity());
                        ps.setLong(12, s.getAskQuantity());

                        ps.setDouble(13, s.getOiConcentration());
                        ps.setDouble(14, s.getObi());

                        ps.setLong(15, s.getTotalTradedVolume());
                        ps.setDouble(16, s.getVolumeConcentration());

                        ps.setDouble(17, s.getImpliedVolatility());

                        ps.setString(18, s.getOptionActivity().name());

                        ps.setTimestamp(
                                19,
                                Timestamp.valueOf(s.getNseTimestamp())
                        );

                        ps.setTimestamp(
                                20,
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
}
