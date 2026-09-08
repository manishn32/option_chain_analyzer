package com.nse.analyser.utils;

public class ClickHouseAnalyticsScripts {
    public static final String POSITION_BUILD_UP = """
            CREATE VIEW option_chain.vw_position_buildup
            AS
            SELECT
            *,
            
            close_oi - previous_oi oi_delta*
            close_ltp - previous_ltp price_de*ta,
            
            multiIf(
            
            close_oi > previous*oi
            AND close_ltp > previous_ltp,
            '*ONG_BUILDUP',
            
            close_oi > previous*oi
            AND close_ltp < previous_ltp,
            '*HORT_BUILDUP',
            
            close_oi < previou*_oi
            AND close_ltp > previous_ltp,
            *SHORT_COVERING',
            
            close_oi < previ*us_oi
            AND close_ltp < previous_ltp*
            'LONG_UNWINDING',
            
            'NEUTRAL'
            
            ) m*rket_structure
            
            FROM vw_day_over_d*y
            """;
}
