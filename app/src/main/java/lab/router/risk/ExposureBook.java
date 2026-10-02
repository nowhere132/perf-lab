package lab.router.risk;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Intraday gross exposure per account (sum of the notional of every order
 * accepted today). Checked on every order before it leaves the building, so
 * it lives in memory.
 */
public class ExposureBook {
    private final ConcurrentHashMap<Long, BigDecimal> exposure = new ConcurrentHashMap<>();

    /** Adds notional to the account's exposure unless that would exceed limit. */
    public boolean tryAdd(long accountId, BigDecimal notional, BigDecimal limit) {
        boolean[] ok = {true};
        exposure.compute(accountId, (id, current) -> {
            BigDecimal next = current == null ? notional : current.add(notional);
            if (next.compareTo(limit) > 0) {
                ok[0] = false;
                return current;
            }
            return next;
        });
        return ok[0];
    }

    public BigDecimal of(long accountId) {
        return exposure.getOrDefault(accountId, BigDecimal.ZERO);
    }

    public BigDecimal total() {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal v : exposure.values()) sum = sum.add(v);
        return sum;
    }
}
