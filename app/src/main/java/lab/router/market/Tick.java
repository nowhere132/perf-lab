package lab.router.market;

import java.math.BigDecimal;

public record Tick(long timestamp, BigDecimal price, long volume) {
}
