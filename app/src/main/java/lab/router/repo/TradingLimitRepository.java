package lab.router.repo;

import lab.router.domain.TradingLimit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradingLimitRepository extends JpaRepository<TradingLimit, String> {
}
