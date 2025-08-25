


// Enhanced TradeRepository.java
package yes.example.no.repository;

import yes.example.no.entity.Account;
import yes.example.no.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findByAccount(Account account);
    List<Trade> findByAccountAndOpenTrue(Account account);
    List<Trade> findByAccountAndOpenFalse(Account account);
    List<Trade> findByAccountAndOpenFalseOrderByCloseTime(Account account);
    
    @Query("SELECT t FROM Trade t WHERE t.account = :account AND t.openTime BETWEEN :start AND :end")
    List<Trade> findByAccountAndOpenTimeBetween(
        @Param("account") Account account, 
        @Param("start") LocalDateTime start, 
        @Param("end") LocalDateTime end
    );
    
    @Query("SELECT SUM(t.realizedProfit) FROM Trade t WHERE t.account = :account AND t.open = false")
    Double sumRealizedProfitByAccount(@Param("account") Account account);
    
    @Query("SELECT COUNT(t) FROM Trade t WHERE t.account = :account AND t.open = false AND t.realizedProfit > 0")
    Long countWinningTradesByAccount(@Param("account") Account account);
    
    @Query("SELECT COUNT(t) FROM Trade t WHERE t.account = :account AND t.open = false AND t.realizedProfit < 0")
    Long countLosingTradesByAccount(@Param("account") Account account);
}
