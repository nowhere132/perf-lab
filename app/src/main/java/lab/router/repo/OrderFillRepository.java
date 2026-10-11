package lab.router.repo;

import lab.router.domain.Fill;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderFillRepository extends JpaRepository<Fill, Long> {
}
