package lab.router.service;

import lab.router.domain.Fill;
import lab.router.domain.Order;
import lab.router.domain.OrderEvent;
import lab.router.repo.OrderRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderHistoryService {
    private final OrderRepository orders;

    public OrderHistoryService(OrderRepository orders) {
        this.orders = orders;
    }

    /** The account's most recent orders with their fill summary, newest first. */
    @Transactional(readOnly = true)
    public List<OrderView> recent(long accountId, int limit) {
        List<OrderView> views = new ArrayList<>();
        for (Order order : orders.findByAccountIdOrderByCreatedAtDesc(accountId, PageRequest.of(0, limit))) {
            int filled = 0;
            BigDecimal paid = BigDecimal.ZERO;
            for (Fill fill : order.getFills()) {
                filled += fill.getQty();
                paid = paid.add(fill.getPrice().multiply(BigDecimal.valueOf(fill.getQty())));
            }
            String lastEvent = null;
            for (OrderEvent event : order.getEvents()) lastEvent = event.getType();

            views.add(new OrderView(order.getId(), order.getSymbol(), order.getSide().name(), order.getQty(),
                    order.getPrice(), order.getStatus().name(), order.getVenue(), order.getCreatedAt(), filled,
                    filled == 0 ? null : paid.divide(BigDecimal.valueOf(filled), 2, RoundingMode.HALF_UP),
                    lastEvent));
        }
        return views;
    }
}
