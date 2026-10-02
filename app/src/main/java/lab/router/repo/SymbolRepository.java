package lab.router.repo;

import lab.router.domain.SymbolEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SymbolRepository extends JpaRepository<SymbolEntity, String> {
}
