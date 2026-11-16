package com.aquariux.technical.assessment.trade.dto.internal;

import lombok.Data;

/**
 * A crypto pair together with the codes of its base and quote symbols.
 */
@Data
public class TradingPairDto {
    private Long id;
    private String pairName;
    private Long baseSymbolId;
    private String baseSymbol;
    private Long quoteSymbolId;
    private String quoteSymbol;
    private Boolean active;
}
