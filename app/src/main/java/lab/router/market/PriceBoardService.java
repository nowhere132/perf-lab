package lab.router.market;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceBoardService {
    private final ReferenceData referenceData;
    private final TickStore tickStore;

    public List<BoardRow> board(String board, int offset, int limit) {
        List<SymbolRef> symbols = referenceData.board(board);
        List<BoardRow> rows = new ArrayList<>();
        for (int i = offset; i < Math.min(symbols.size(), offset + limit); i++) {
            rows.add(row(symbols.get(i)));
        }
        return rows;
    }

    private BoardRow row(SymbolRef s) {
        List<Tick> ticks = tickStore.recent(s.symbol());
        DecimalFormat money = new DecimalFormat("#,##0.00");
        DecimalFormat pct = new DecimalFormat("+0.00;-0.00");

        BigDecimal high = null, low = null, last = s.refPrice();
        BigDecimal turnover = BigDecimal.ZERO;
        long volume = 0;
        for (Tick t : ticks) {
            if (high == null || t.price().compareTo(high) > 0) high = t.price();
            if (low == null || t.price().compareTo(low) < 0) low = t.price();
            turnover = turnover.add(t.price().multiply(BigDecimal.valueOf(t.volume())));
            volume += t.volume();
            last = t.price();
        }
        BigDecimal vwap = volume == 0 ? s.refPrice()
                : turnover.divide(BigDecimal.valueOf(volume), 2, RoundingMode.HALF_UP);
        BigDecimal change = last.subtract(s.refPrice())
                .multiply(BigDecimal.valueOf(100))
                .divide(s.refPrice(), 2, RoundingMode.HALF_UP);

        return new BoardRow(s.symbol(), money.format(s.refPrice()), money.format(s.ceiling()), money.format(s.floor()),
                money.format(last), money.format(high == null ? s.refPrice() : high),
                money.format(low == null ? s.refPrice() : low), money.format(vwap), volume, pct.format(change));
    }
}
