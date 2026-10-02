package lab.router.market;

import lab.router.domain.SymbolEntity;
import lab.router.domain.TradingLimit;
import lab.router.repo.SymbolRepository;
import lab.router.repo.TradingLimitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Symbols, price bands and trading limits, held in memory so the order path
 * never has to query them. Operations can change limits intraday, so the
 * cache is reloaded on a timer (router.refdata.refresh-interval).
 */
@Component
public class ReferenceData {
    private static final Logger log = LoggerFactory.getLogger(ReferenceData.class);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final SymbolRepository symbols;
    private final TradingLimitRepository limits;
    private final Map<String, SymbolRef> bySymbol = new HashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public ReferenceData(SymbolRepository symbols, TradingLimitRepository limits) {
        this.symbols = symbols;
        this.limits = limits;
    }

    @PostConstruct
    @Scheduled(initialDelayString = "${router.refdata.refresh-interval}",
               fixedDelayString = "${router.refdata.refresh-interval}")
    public void reload() {
        long start = System.nanoTime();
        lock.writeLock().lock();
        try {
            bySymbol.clear();
            for (SymbolEntity s : symbols.findAll()) {
                TradingLimit limit = limits.findById(s.getSymbol()).orElseThrow();
                BigDecimal band = s.getRefPrice().multiply(s.getBandPct()).divide(HUNDRED, 0, RoundingMode.DOWN);
                bySymbol.put(s.getSymbol(), new SymbolRef(
                        s.getSymbol(), s.getBoard(), s.getRefPrice(),
                        roundToTick(s.getRefPrice().add(band), s.getTickSize()),
                        roundToTick(s.getRefPrice().subtract(band), s.getTickSize()),
                        s.getTickSize(), s.getLotSize(), limit.getMaxOrderQty()));
            }
        } finally {
            lock.writeLock().unlock();
        }
        log.debug("reference data reloaded: {} symbols in {} ms", bySymbol.size(), (System.nanoTime() - start) / 1_000_000);
    }

    public SymbolRef find(String symbol) {
        lock.readLock().lock();
        try {
            return bySymbol.get(symbol);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<SymbolRef> board(String board) {
        lock.readLock().lock();
        try {
            List<SymbolRef> out = new ArrayList<>();
            for (SymbolRef s : bySymbol.values()) {
                if (s.board().equals(board)) out.add(s);
            }
            out.sort(Comparator.comparing(SymbolRef::symbol));
            return out;
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<String> allSymbols() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(bySymbol.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }

    private static BigDecimal roundToTick(BigDecimal price, BigDecimal tick) {
        return price.divide(tick, 0, RoundingMode.DOWN).multiply(tick);
    }
}
