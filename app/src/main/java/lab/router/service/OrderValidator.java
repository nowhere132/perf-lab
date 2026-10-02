package lab.router.service;

import lab.router.market.SymbolRef;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OrderValidator {
    public void validate(PlaceOrderCommand cmd, SymbolRef symbol) {
        if (cmd.qty() <= 0 || cmd.qty() % symbol.lotSize() != 0)
            throw new OrderRejectedException("quantity must be a positive multiple of " + symbol.lotSize());
        if (cmd.qty() > symbol.maxOrderQty())
            throw new OrderRejectedException("quantity above the per-order limit");
        if (cmd.price().compareTo(symbol.floor()) < 0 || cmd.price().compareTo(symbol.ceiling()) > 0)
            throw new OrderRejectedException("price outside the daily band");
        if (cmd.price().remainder(symbol.tickSize()).compareTo(BigDecimal.ZERO) != 0)
            throw new OrderRejectedException("price is not on a tick");
    }
}
