package yes.example.no.repository;

import yes.example.no.entity.Account;
import yes.example.no.entity.BalanceAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BalanceAdjustmentRepository extends JpaRepository<BalanceAdjustment, Long> {
    List<BalanceAdjustment> findByAccountOrderByAdjustmentTime(Account account);
    List<BalanceAdjustment> findByAdjustmentTimeBetween(LocalDateTime start, LocalDateTime end);
    List<BalanceAdjustment> findByAdjustedBy(String adjustedBy);
}
