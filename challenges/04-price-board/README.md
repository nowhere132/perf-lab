# 04 · The price board stutters

**Situation.** `GET /api/market/board/{board}` serves one page of the price
board: last, high, low, volume and VWAP for 30 symbols, derived from the last
1,000 trades of each. Trading screens poll it constantly. Most responses take
a couple of milliseconds; every few seconds the board freezes for half a
second or more.

The service runs with the settings in
`challenges/04-price-board/compose.override.yaml`.

**Goal.** `./lab test` offers 300 requests/s.

| | target |
|---|---|
| p99 latency | < 20 ms |
| p99.9 latency | < 60 ms |
| errors | < 0.1 % |

**Rules.**
- Memory budget for the app container: 1 GiB (the grader checks).
- The market is not yours to slow down: `ROUTER_MARKET_TRADES_PER_SECOND`
  and `ticks-per-symbol` stay as they are, and the board must still be
  computed from the last 1,000 trades.

**You may change** the overlay file (then `./lab restart app`) and anything
under `app/` (then `./lab deploy`).

**Instruments worth reaching for.** The JVM row in Grafana, `./lab gc`,
`./lab exec jcmd 1 VM.flags`, `./lab exec jcmd 1 GC.heap_info`, GC logging
(`-Xlog:gc*:file=/results/gc.log`), `./lab jfr`, `./lab profile alloc`.

**Afterwards you should be able to say**
- which collector was running, and who chose it;
- what kind of object causes the long pauses, and why objects of that age are the expensive ones;
- what your fix costs (memory, CPU, or code complexity).

**Stretch.** Pass with a 256 MB heap (`-Xmx256m`). That one cannot be done with flags.
