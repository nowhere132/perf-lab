package lab.router.repo;

import lab.router.domain.Fill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface OrderFillRepository extends JpaRepository<Fill, Long> {
    record TotalFill(long filled, BigDecimal paid) {}

    @Query("select coalesce(sum(f.qty), 0) as filled, coalesce(sum(f.price * f.qty), 0) as paid " +
            "from Fill f where f.order.id = :orderId")
    TotalFill getTotalFill(long orderId);
}
