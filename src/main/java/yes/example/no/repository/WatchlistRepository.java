package yes.example.no.repository;

import yes.example.no.entity.Account;
import yes.example.no.entity.Symbol;
import yes.example.no.entity.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {
    List<Watchlist> findByAccountOrderBySortOrder(Account account);
    Optional<Watchlist> findByAccountAndSymbol(Account account, Symbol symbol);
    
    @Query("SELECT COALESCE(MAX(w.sortOrder), -1) FROM Watchlist w WHERE w.account = :account")
    Optional<Integer> findMaxSortOrderByAccount(@Param("account") Account account);
    
    void deleteByAccountAndSymbol(Account account, Symbol symbol);
}
