package yes.example.no.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import yes.example.no.entity.Group;
import yes.example.no.entity.GroupSymbol;
import yes.example.no.entity.Symbol;
import yes.example.no.entity.Account;
import yes.example.no.repository.GroupRepository;
import yes.example.no.repository.GroupSymbolRepository;
import yes.example.no.repository.SymbolRepository;
import yes.example.no.repository.AccountRepository;

import java.util.List;
import java.util.Optional;

@Service
public class AdminGroupService {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupSymbolRepository groupSymbolRepository;

    @Autowired
    private SymbolRepository symbolRepository;

    @Autowired
    private AccountRepository accountRepository;

    // Group CRUD operations
    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }

    public Optional<Group> getGroupById(Long id) {
        return groupRepository.findById(id);
    }

    public Group createGroup(Group group) {
        return groupRepository.save(group);
    }

    public Group updateGroup(Long id, Group updatedGroup) {
        return groupRepository.findById(id)
                .map(group -> {
                    group.setName(updatedGroup.getName());
                    group.setDescription(updatedGroup.getDescription());
                    group.setActive(updatedGroup.isActive());
                    return groupRepository.save(group);
                })
                .orElseThrow(() -> new RuntimeException("Group not found with id: " + id));
    }

    public void deleteGroup(Long id) {
        // First remove all members from the group
        List<Account> members = accountRepository.findByGroupId(id);
        members.forEach(account -> {
            account.setGroup(null);
            accountRepository.save(account);
        });

        // Then delete all group symbols
        List<GroupSymbol> groupSymbols = groupSymbolRepository.findByGroupId(id);
        groupSymbolRepository.deleteAll(groupSymbols);

        // Finally delete the group
        groupRepository.deleteById(id);
    }

    // Group Symbol operations
    public List<GroupSymbol> getGroupSymbols(Long groupId) {
        return groupSymbolRepository.findByGroupId(groupId);
    }

    public List<Symbol> getAvailableSymbols(Long groupId) {
        List<Symbol> allSymbols = symbolRepository.findAll();
        List<GroupSymbol> groupSymbols = groupSymbolRepository.findByGroupId(groupId);
        
        // Filter out symbols that are already in the group
        return allSymbols.stream()
                .filter(symbol -> groupSymbols.stream()
                        .noneMatch(gs -> gs.getSymbol().getId().equals(symbol.getId())))
                .toList();
    }

    public GroupSymbol addSymbolToGroup(Long groupId, GroupSymbol groupSymbol) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        Symbol symbol = symbolRepository.findById(groupSymbol.getSymbol().getId())
                .orElseThrow(() -> new RuntimeException("Symbol not found"));
        
        groupSymbol.setGroup(group);
        groupSymbol.setSymbol(symbol);
        
        return groupSymbolRepository.save(groupSymbol);
    }

    public GroupSymbol updateGroupSymbol(Long groupId, Long groupSymbolId, GroupSymbol updatedGroupSymbol) {
        return groupSymbolRepository.findById(groupSymbolId)
                .map(gs -> {
                    gs.setCanViewQuotes(updatedGroupSymbol.isCanViewQuotes());
                    gs.setCanTrade(updatedGroupSymbol.isCanTrade());
                    gs.setBidMarkup(updatedGroupSymbol.getBidMarkup());
                    gs.setAskMarkup(updatedGroupSymbol.getAskMarkup());
                    gs.setBidMarkupPercent(updatedGroupSymbol.getBidMarkupPercent());
                    gs.setAskMarkupPercent(updatedGroupSymbol.getAskMarkupPercent());
                    return groupSymbolRepository.save(gs);
                })
                .orElseThrow(() -> new RuntimeException("Group symbol not found"));
    }

    public void removeSymbolFromGroup(Long groupId, Long symbolId) {
        GroupSymbol groupSymbol = groupSymbolRepository.findByGroupIdAndSymbolId(groupId, symbolId)
                .orElseThrow(() -> new RuntimeException("Group symbol not found"));
        
        groupSymbolRepository.delete(groupSymbol);
    }
    
    //get available symbols for group
    public List<Symbol> getAvailableSymbolsForGroup(Long groupId) {
        List<Symbol> allSymbols = symbolRepository.findAll();
        List<GroupSymbol> groupSymbols = groupSymbolRepository.findByGroupId(groupId);
        
        return allSymbols.stream()
                .filter(symbol -> groupSymbols.stream()
                        .noneMatch(gs -> gs.getSymbol().getId().equals(symbol.getId())))
                .toList();
    }

    // Group Member operations
    public List<Account> getGroupMembers(Long groupId) {
        return accountRepository.findByGroupId(groupId);
    }

    public List<Account> getUnassignedAccounts() {
        return accountRepository.findByGroupIsNull();
    }

    public void assignUserToGroup(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        Account account = accountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        account.setGroup(group);
        accountRepository.save(account);
    }

    public void removeUserFromGroup(Long userId) {
        Account account = accountRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        account.setGroup(null);
        accountRepository.save(account);
    }

    public void bulkAssignUsersToGroup(Long groupId, List<Long> userIds) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        List<Account> accounts = accountRepository.findAllById(userIds);
        accounts.forEach(account -> account.setGroup(group));
        accountRepository.saveAll(accounts);
    }

    // Utility methods
    public long getGroupMemberCount(Long groupId) {
        return accountRepository.countByGroupId(groupId);
    }

    public long getGroupSymbolCount(Long groupId) {
        return groupSymbolRepository.countByGroupId(groupId);
    }
}