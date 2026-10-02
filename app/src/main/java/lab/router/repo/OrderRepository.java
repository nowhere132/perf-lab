package lab.router.repo;

import lab.router.domain.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable page);
}
