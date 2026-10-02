package lab.router.market;

import java.math.BigDecimal;

public record SymbolRef(String symbol, String board, BigDecimal refPrice, BigDecimal ceiling, BigDecimal floor,
                        BigDecimal tickSize, int lotSize, int maxOrderQty) {
}
