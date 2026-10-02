package lab.router.market;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The most recent trades of every symbol, used to derive the price board
 * (last / high / low / volume / VWAP). There is no real market feed in the
 * lab, so a timer plays that role and prints trades at the rate of a busy
 * session (router.market.trades-per-second).
 */
@Component
public class TickStore {
    private final ReferenceData referenceData;
    private final int ticksPerSymbol;
    private final int tradesPerBeat;
    private final Map<String, LinkedList<Tick>> ticks = new ConcurrentHashMap<>();
    private volatile List<SymbolRef> universe = List.of();

    public TickStore(ReferenceData referenceData,
                     @Value("${router.market.ticks-per-symbol}") int ticksPerSymbol,
                     @Value("${router.market.trades-per-second}") int tradesPerSecond) {
        this.referenceData = referenceData;
        this.ticksPerSymbol = ticksPerSymbol;
        this.tradesPerBeat = tradesPerSecond / 10;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        List<SymbolRef> all = new ArrayList<>();
        for (String board : List.of("HOSE", "HNX", "UPCOM")) all.addAll(referenceData.board(board));
        long now = System.currentTimeMillis();
        for (SymbolRef s : all) {
            LinkedList<Tick> list = new LinkedList<>();
            for (int i = 0; i < ticksPerSymbol; i++) list.add(randomTick(s, now - (ticksPerSymbol - i) * 1000L));
            ticks.put(s.symbol(), list);
        }
        universe = all;
    }

    @Scheduled(fixedRate = 100)
    public void feed() {
        List<SymbolRef> all = universe;
        if (all.isEmpty()) return;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        long now = System.currentTimeMillis();
        for (int i = 0; i < tradesPerBeat; i++) {
            SymbolRef s = all.get(rnd.nextInt(all.size()));
            LinkedList<Tick> list = ticks.get(s.symbol());
            synchronized (list) {
                list.addLast(randomTick(s, now));
                while (list.size() > ticksPerSymbol) list.removeFirst();
            }
        }
    }

    /** Snapshot of a symbol's recent trades, oldest first. */
    public List<Tick> recent(String symbol) {
        LinkedList<Tick> list = ticks.get(symbol);
        if (list == null) return List.of();
        synchronized (list) {
            return new ArrayList<>(list);
        }
    }

    private static Tick randomTick(SymbolRef s, long ts) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        long steps = s.ceiling().subtract(s.floor()).divide(s.tickSize()).longValue();
        BigDecimal price = s.floor().add(s.tickSize().multiply(BigDecimal.valueOf(rnd.nextLong(steps + 1))));
        return new Tick(ts, price, (1 + rnd.nextInt(50)) * 100L);
    }
}
