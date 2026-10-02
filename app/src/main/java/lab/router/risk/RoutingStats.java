package lab.router.risk;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLongArray;

/** Orders and notional routed per board today. Feeds the ops dashboard. */
public class RoutingStats {
    public static final int HOSE = 0, HNX = 1, UPCOM = 2;

    private final AtomicLongArray orders = new AtomicLongArray(3);
    private final AtomicLongArray notional = new AtomicLongArray(3);

    public static int boardIndex(String board) {
        return switch (board) {
            case "HOSE" -> HOSE;
            case "HNX" -> HNX;
            default -> UPCOM;
        };
    }

    public void record(int board, BigDecimal orderNotional) {
        orders.incrementAndGet(board);
        notional.addAndGet(board, orderNotional.longValue());
    }

    public long orders(int board) { return orders.get(board); }
    public long notional(int board) { return notional.get(board); }
}
