package yes.example.no.repository;

import yes.example.no.entity.Account;
import yes.example.no.entity.Symbol;
import yes.example.no.entity.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

    @Query("SELECT COALESCE(MAX(w.orderIndex), -1) FROM Watchlist w WHERE w.account = :account")
    Integer findMaxOrderIndexByAccount(@Param("account") Account account);

    List<Watchlist> findByAccount(Account account);

    List<Watchlist> findByAccountOrderByOrderIndexAsc(Account account);

    List<Watchlist> findByAccountIdOrderByOrderIndexAsc(Long accountId);

    Optional<Watchlist> findByAccountAndSymbol(Account account, Symbol symbol);

    Optional<Watchlist> findByAccountIdAndSymbolId(Long accountId, Long symbolId);

    void deleteByAccountAndSymbol(Account account, Symbol symbol);

    void deleteByAccountIdAndSymbolId(Long accountId, Long symbolId);

    @Query("SELECT COUNT(w) FROM Watchlist w WHERE w.account = :account")
    int countByAccount(@Param("account") Account account);
}
