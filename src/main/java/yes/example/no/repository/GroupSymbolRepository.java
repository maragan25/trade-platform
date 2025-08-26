package yes.example.no.repository;

import yes.example.no.entity.Group;
import yes.example.no.entity.GroupSymbol;
import yes.example.no.entity.Symbol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupSymbolRepository extends JpaRepository<GroupSymbol, Long> {
    Optional<GroupSymbol> findByGroupAndSymbol(Group group, Symbol symbol);
    List<GroupSymbol> findByGroup(Group group);
    
    @Query("SELECT gs.symbol FROM GroupSymbol gs WHERE gs.group = :group AND gs.canViewQuotes = true")
    List<Symbol> findSymbolsByGroupAndCanViewQuotesTrue(@Param("group") Group group);
    
    @Query("SELECT gs.symbol FROM GroupSymbol gs WHERE gs.group = :group AND gs.canTrade = true")
    List<Symbol> findSymbolsByGroupAndCanTradeTrue(@Param("group") Group group);
    
    void deleteByGroupAndSymbol(Group group, Symbol symbol);
    List<GroupSymbol> findByGroupId(Long groupId);
    @Query("SELECT gs FROM GroupSymbol gs WHERE gs.group.id = :groupId AND gs.symbol.id = :symbolId")
    Optional<GroupSymbol> findByGroupIdAndSymbolId(Long groupId, Long symbolId);
    long countByGroupId(Long groupId);
    
    
}
