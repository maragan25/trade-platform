package yes.example.no.repository;

import yes.example.no.entity.Account;
import yes.example.no.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountOrderByTimestamp(Account account);
    List<Transaction> findByAccountAndTimestampBetween(Account account, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.account = :account")
    Double sumTransactionsByAccount(@Param("account") Account account);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.account = :account AND t.type = :type")
    Double sumTransactionsByAccountAndType(@Param("account") Account account, @Param("type") String type);
}
