package yes.example.no.repository;

import yes.example.no.entity.Account;
import yes.example.no.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUsername(String username);
    List<Account> findByGroup(Group group);
    List<Account> findByGroupIsNull();
    List<Account> findByGroupIsNullAndIsAdminFalse();
    List<Account> findByIsAdminFalse();
    List<Account> findByIsAdminTrue();
    Optional<Account> findByUsernameAndIsAdminTrue(String username);
}
