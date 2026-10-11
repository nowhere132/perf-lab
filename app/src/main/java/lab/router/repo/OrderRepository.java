package lab.router.repo;

import lab.router.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    /** One row of the order book: the order plus its fill totals and latest event type. */
    interface OrderSummary {
        Long getId();
        String getSymbol();
        String getSide();
        Integer getQty();
        BigDecimal getPrice();
        String getStatus();
        String getVenue();
        Instant getCreatedAt();
        Long getFilledQty();
        BigDecimal getPaid();
        String getLastEvent();
    }

    /** The account's newest orders with fill totals and last event, in one statement. */
    @Query(nativeQuery = true, value = """
            select o.id, o.symbol, o.side, o.qty, o.price, o.status, o.venue,
                   o.created_at as "createdAt",
                   coalesce(f.filled, 0) as "filledQty",
                   coalesce(f.paid, 0)   as "paid",
                   e.type                as "lastEvent"
            from (select * from orders
                  where account_id = :accountId
                  order by created_at desc
                  limit :limit) o
            left join lateral (select sum(qty) as filled, sum(price * qty) as paid
                               from fills where order_id = o.id) f on true
            left join lateral (select type from order_events
                               where order_id = o.id
                               order by id desc limit 1) e on true
            order by o.created_at desc
            """)
    List<OrderSummary> findRecentSummaries(@Param("accountId") long accountId, @Param("limit") int limit);
}
