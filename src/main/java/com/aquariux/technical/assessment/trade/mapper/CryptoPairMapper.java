package com.aquariux.technical.assessment.trade.mapper;

import com.aquariux.technical.assessment.trade.dto.internal.TradingPairDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CryptoPairMapper {

    @Select("""
            SELECT id FROM crypto_pairs WHERE pair_name = #{pairName}
            """)
    Long findIdByPairName(String pairName);

    @Select("""
            SELECT cp.id, cp.pair_name, cp.active,
                   base.id AS base_symbol_id, base.symbol AS base_symbol,
                   quote.id AS quote_symbol_id, quote.symbol AS quote_symbol
            FROM crypto_pairs cp
            INNER JOIN symbols base ON base.id = cp.base_symbol_id
            INNER JOIN symbols quote ON quote.id = cp.quote_symbol_id
            WHERE cp.pair_name = #{pairName}
            """)
    TradingPairDto findTradingPairByName(String pairName);
}
