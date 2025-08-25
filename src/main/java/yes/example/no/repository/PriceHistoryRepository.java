package yes.example.no.repository;

import yes.example.no.entity.PriceHistory;
import yes.example.no.entity.Symbol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
    List<PriceHistory> findBySymbolAndTimestampBetweenOrderByTimestamp(
        Symbol symbol, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT ph FROM PriceHistory ph WHERE ph.symbol = :symbol ORDER BY ph.timestamp DESC")
    List<PriceHistory> findLatestBySymbol(Symbol symbol);
}
