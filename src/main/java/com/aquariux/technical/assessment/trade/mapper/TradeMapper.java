package com.aquariux.technical.assessment.trade.mapper;

import com.aquariux.technical.assessment.trade.entity.Trade;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Trades are an immutable audit trail: this mapper deliberately exposes no update or delete operations.
 */
@Mapper
public interface TradeMapper {

    @Insert("""
            INSERT INTO trades (user_id, crypto_pair_id, trade_type, quantity, price, total_amount, trade_time)
            VALUES (#{userId}, #{cryptoPairId}, #{tradeType}, #{quantity}, #{price}, #{totalAmount}, #{tradeTime})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Trade trade);

    @Select("""
            SELECT id, user_id, crypto_pair_id, trade_type, quantity, price, total_amount, trade_time
            FROM trades WHERE id = #{id}
            """)
    Trade findById(Long id);

    @Select("""
            SELECT id, user_id, crypto_pair_id, trade_type, quantity, price, total_amount, trade_time
            FROM trades WHERE user_id = #{userId}
            ORDER BY trade_time DESC, id DESC
            """)
    List<Trade> findByUserId(Long userId);
}
