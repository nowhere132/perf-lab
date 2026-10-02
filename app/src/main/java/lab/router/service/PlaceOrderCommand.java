package lab.router.service;

import lab.router.domain.Side;

import java.math.BigDecimal;

public record PlaceOrderCommand(long accountId, String symbol, Side side, int qty, BigDecimal price) {
}
