package com.aquariux.technical.assessment.trade.mapper;

import com.aquariux.technical.assessment.trade.entity.CryptoPrice;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CryptoPriceMapper {

    @Insert("""
            INSERT INTO crypto_prices (crypto_pair_id, bid_price, ask_price, bid_source, ask_source)
            VALUES (#{cryptoPairId}, #{bidPrice}, #{askPrice}, #{bidSource}, #{askSource})
            """)
    void insertPrice(CryptoPrice cryptoPrice);

    // TODO: Pre-existing issue (left as-is on request). The "latest" row is chosen by
    //  created_at = MAX(created_at). If two price rows for the same pair share the exact same timestamp,
    //  both are returned and /api/prices/latest shows that pair twice. Picking one row per pair
    //  (e.g. ORDER BY created_at DESC, id DESC with a window function) would avoid the duplicates.
    @Select("""
            SELECT cp.id as id, cp.crypto_pair_id as cryptoPairId, cp.bid_price as bidPrice, cp.ask_price as askPrice,
                   cp.bid_source as bidSource, cp.ask_source as askSource, cp.created_at as createdAt, pair.pair_name as pairName
            FROM crypto_prices cp
            INNER JOIN crypto_pairs pair ON cp.crypto_pair_id = pair.id
            WHERE cp.created_at = (SELECT MAX(created_at) FROM crypto_prices cp2 WHERE cp2.crypto_pair_id = cp.crypto_pair_id)
            ORDER BY cp.crypto_pair_id
            """)
    List<CryptoPrice> findLatestPrices();

    /**
     * Latest quote for one pair; ties on created_at are broken by the highest id (last inserted).
     */
    @Select("""
            SELECT id, crypto_pair_id, bid_price, ask_price, bid_source, ask_source, created_at
            FROM crypto_prices
            WHERE crypto_pair_id = #{cryptoPairId}
            ORDER BY created_at DESC, id DESC
            LIMIT 1
            """)
    CryptoPrice findLatestByPairId(Long cryptoPairId);
}
