package lab.router.service;

import lab.router.repo.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderHistoryService {
    private final OrderRepository orders;

    /** The account's most recent orders with their fill summary, newest first. */
    @Transactional(readOnly = true)
    public List<OrderView> recent(long accountId, int limit) {
        return orders.findRecentSummaries(accountId, limit).stream().map(row -> {
            int filled = row.getFilledQty().intValue();
            return new OrderView(row.getId(), row.getSymbol(), row.getSide(), row.getQty(),
                    row.getPrice(), row.getStatus(), row.getVenue(), row.getCreatedAt(), filled,
                    filled == 0 ? null : row.getPaid().divide(BigDecimal.valueOf(filled), 2, RoundingMode.HALF_UP),
                    row.getLastEvent());
        }).toList();
    }
}
