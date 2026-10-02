package lab.router.bench;

import lab.router.risk.ExposureBook;
import lab.router.risk.RoutingStats;

import java.math.BigDecimal;
import java.util.SplittableRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

/**
 * The in-memory part of the order path, with everything else (HTTP, SQL, the
 * venue) taken away: one dispatcher thread per board, each pushing orders for
 * random accounts through the exposure check and the routing counters.
 *
 * Part of the grader -- do not edit. Run it with ./lab bench.
 */
public final class HotPathBench {
    static final int ACCOUNTS = 1_000_000;
    static final int THREADS = 3;                 // HOSE, HNX, UPCOM
    static final int ROUNDS = 8, WARMUP_ROUNDS = 4;
    static final long ROUND_NANOS = 2_000_000_000L;
    static final BigDecimal LIMIT = new BigDecimal("1000000000000000");

    public static void main(String[] args) throws Exception {
        ExposureBook book = new ExposureBook();
        RoutingStats stats = new RoutingStats();
        BigDecimal[] notionals = new BigDecimal[64];
        for (int i = 0; i < notionals.length; i++) notionals[i] = BigDecimal.valueOf((i + 1) * 1_000_000L);

        long[] ops = new long[THREADS], sums = new long[THREADS];
        double[] scores = new double[ROUNDS];

        for (int round = 0; round < ROUNDS; round++) {
            long[] roundOps = new long[THREADS], roundSums = new long[THREADS];
            CyclicBarrier go = new CyclicBarrier(THREADS);
            CountDownLatch done = new CountDownLatch(THREADS);
            for (int t = 0; t < THREADS; t++) {
                final int board = t, seed = round * 31 + t;
                Thread.ofPlatform().name("dispatcher-" + t).start(() -> {
                    SplittableRandom rnd = new SplittableRandom(seed);
                    long n = 0, sum = 0;
                    try {
                        go.await();
                        long end = System.nanoTime() + ROUND_NANOS;
                        do {
                            for (int i = 0; i < 1024; i++) {
                                BigDecimal notional = notionals[rnd.nextInt(notionals.length)];
                                if (book.tryAdd(1 + rnd.nextInt(ACCOUNTS), notional, LIMIT)) {
                                    stats.record(board, notional);
                                    sum += notional.longValue();
                                    n++;
                                }
                            }
                        } while (System.nanoTime() < end);
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    } finally {
                        roundOps[board] = n;
                        roundSums[board] = sum;
                        done.countDown();
                    }
                });
            }
            done.await();
            long total = 0;
            for (int t = 0; t < THREADS; t++) {
                ops[t] += roundOps[t];
                sums[t] += roundSums[t];
                total += roundOps[t];
            }
            scores[round] = total / (ROUND_NANOS / 1e9) / 1e6;
            System.out.printf("  round %d%s  %7.2f M ops/s%n", round + 1, round < WARMUP_ROUNDS ? " (warm-up)" : "          ", scores[round]);
        }

        // Fast and wrong is worth nothing: every order must be counted exactly once.
        long totalSum = 0;
        StringBuilder wrong = new StringBuilder();
        for (int t = 0; t < THREADS; t++) {
            totalSum += sums[t];
            if (stats.orders(t) != ops[t]) wrong.append(" orders[").append(t).append("]=").append(stats.orders(t)).append(" expected ").append(ops[t]);
            if (stats.notional(t) != sums[t]) wrong.append(" notional[").append(t).append("] off by ").append(stats.notional(t) - sums[t]);
        }
        if (book.total().compareTo(BigDecimal.valueOf(totalSum)) != 0) wrong.append(" exposure total=").append(book.total()).append(" expected ").append(totalSum);
        if (book.tryAdd(1, LIMIT, LIMIT)) wrong.append(" limit not enforced");

        double best = 0;
        for (int r = WARMUP_ROUNDS; r < ROUNDS; r++) best += scores[r];
        System.out.printf("SCORE %.2f%n", best / (ROUNDS - WARMUP_ROUNDS));
        System.out.println(wrong.isEmpty() ? "CHECK ok" : "CHECK FAILED:" + wrong);
    }
}
