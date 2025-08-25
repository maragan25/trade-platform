package yes.example.no.repository;

import yes.example.no.entity.Symbol;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.Optional;

public interface SymbolRepository extends JpaRepository<Symbol, Long> {
    Optional<Symbol> findByName(String name);
        List<Symbol> findByActiveTrue();
}
