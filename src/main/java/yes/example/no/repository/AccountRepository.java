package yes.example.no.repository;

import yes.example.no.entity.Account;
import yes.example.no.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUsername(String username);
    List<Account> findByGroup(Group group);
    List<Account> findByGroupIsNull();
    List<Account> findByGroupIsNullAndIsAdminFalse();
    List<Account> findByIsAdminFalse();
    List<Account> findByIsAdminTrue();
    List<Account> findByGroupId(Long groupId);
    Optional<Account> findByUsernameAndIsAdminTrue(String username);
    long countByGroupId(Long groupId);            
    
    @Query("SELECT a FROM Account a WHERE a.isAdmin = false")
    List<Account> findNonAdminAccounts();
    
    @Query("SELECT a FROM Account a WHERE a.group IS NULL AND a.isAdmin = false")
    List<Account> findUnassignedNonAdminAccounts();
}

