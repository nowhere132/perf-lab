package lab.router.repo;

import lab.router.domain.OrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderEventRepository extends JpaRepository<OrderEvent, Long> {
    @Query("select e.type from OrderEvent e where e.order.id = :orderId order by e.id desc limit 1")
    String getLastEventTypeOf(long orderId);
}
