package lab.router.service;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderView(long id, String symbol, String side, int qty, BigDecimal price, String status, String venue,
                        Instant createdAt, int filledQty, BigDecimal avgFillPrice, String lastEvent) {
}
