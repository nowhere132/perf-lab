package lab.router.service;

import lab.router.domain.Order;
import lab.router.repo.OrderEventRepository;
import lab.router.repo.OrderFillRepository;
import lab.router.repo.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderHistoryService {
    private final OrderRepository orders;
    private final OrderFillRepository orderFills;
    private final OrderEventRepository orderEvents;

    /** The account's most recent orders with their fill summary, newest first. */
    @Transactional(readOnly = true)
    public List<OrderView> recent(long accountId, int limit) {
        List<OrderView> views = new ArrayList<>();
        for (Order order : orders.findByAccountIdOrderByCreatedAtDesc(accountId, PageRequest.of(0, limit))) {
            var totalFill = orderFills.getTotalFill(order.getId());
            int filled = (int) totalFill.filled();
            BigDecimal paid = totalFill.paid();

            String lastEvent = orderEvents.getLastEventTypeOf(order.getId());

            views.add(new OrderView(order.getId(), order.getSymbol(), order.getSide().name(), order.getQty(),
                    order.getPrice(), order.getStatus().name(), order.getVenue(), order.getCreatedAt(), filled,
                    filled == 0 ? null : paid.divide(BigDecimal.valueOf(filled), 2, RoundingMode.HALF_UP),
                    lastEvent));
        }
        return views;
    }
}
