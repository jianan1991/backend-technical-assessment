package com.aquariux.technical.assessment.trade.mapper;

import com.aquariux.technical.assessment.trade.dto.internal.UserWalletDto;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserWalletMapper {

    @Select("""
            SELECT s.symbol, s.name, uw.balance
            FROM symbols s
            INNER JOIN user_wallets uw ON s.id = uw.symbol_id AND uw.user_id = #{userId}
            ORDER BY s.symbol
            """)
    List<UserWalletDto> findByUserId(Long userId);

    @Select("""
            SELECT balance FROM user_wallets WHERE user_id = #{userId} AND symbol_id = #{symbolId}
            """)
    BigDecimal findBalance(@Param("userId") Long userId, @Param("symbolId") Long symbolId);

    /**
     * Atomically subtracts {@code amount} only if the wallet holds enough.
     * The balance check and the write happen in one statement, so concurrent trades can never overdraw.
     *
     * @return 1 if debited, 0 if the wallet is missing or the balance is insufficient
     */
    @Update("""
            UPDATE user_wallets
            SET balance = balance - #{amount}, updated_at = #{updatedAt}
            WHERE user_id = #{userId} AND symbol_id = #{symbolId} AND balance >= #{amount}
            """)
    int debit(@Param("userId") Long userId, @Param("symbolId") Long symbolId,
              @Param("amount") BigDecimal amount, @Param("updatedAt") LocalDateTime updatedAt);

    /**
     * Atomically adds {@code amount} to an existing wallet.
     *
     * @return 1 if credited, 0 if the user has no wallet for this symbol yet
     */
    @Update("""
            UPDATE user_wallets
            SET balance = balance + #{amount}, updated_at = #{updatedAt}
            WHERE user_id = #{userId} AND symbol_id = #{symbolId}
            """)
    int credit(@Param("userId") Long userId, @Param("symbolId") Long symbolId,
               @Param("amount") BigDecimal amount, @Param("updatedAt") LocalDateTime updatedAt);

    @Insert("""
            INSERT INTO user_wallets (user_id, symbol_id, balance, updated_at)
            VALUES (#{userId}, #{symbolId}, #{balance}, #{updatedAt})
            """)
    int insert(@Param("userId") Long userId, @Param("symbolId") Long symbolId,
               @Param("balance") BigDecimal balance, @Param("updatedAt") LocalDateTime updatedAt);
}
