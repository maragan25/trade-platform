package yes.example.no.service;

import yes.example.no.entity.*;
import yes.example.no.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupManagementService {
    
    private final GroupRepository groupRepo;
    private final GroupSymbolRepository groupSymbolRepo;
    private final SymbolRepository symbolRepo;
    private final AccountRepository accountRepo;
    private final WatchlistRepository watchlistRepo;

    public class GroupStats {
        public final long totalGroups;
        public final long activeGroups;
        public final long totalMembers;
        public final long unassignedAccounts;
        
        public GroupStats(long totalGroups, long activeGroups, long totalMembers, long unassignedAccounts) {
            this.totalGroups = totalGroups;
            this.activeGroups = activeGroups;
            this.totalMembers = totalMembers;
            this.unassignedAccounts = unassignedAccounts;
        }
    }

    public GroupStats getGroupStatistics() {
        long totalGroups = groupRepo.count();
        long activeGroups = groupRepo.findByActiveTrue().size();
        long totalMembers = accountRepo.findAll().stream()
                .filter(acc -> !acc.isAdmin() && acc.getGroup() != null)
                .count();
        long unassignedAccounts = accountRepo.findByGroupIsNull().stream()
                .filter(acc -> !acc.isAdmin())
                .count();
        
        return new GroupStats(totalGroups, activeGroups, totalMembers, unassignedAccounts);
    }

    public void cloneGroupConfiguration(Long sourceGroupId, Long targetGroupId) {
        Group sourceGroup = groupRepo.findById(sourceGroupId)
                .orElseThrow(() -> new RuntimeException("Source group not found"));
        Group targetGroup = groupRepo.findById(targetGroupId)
                .orElseThrow(() -> new RuntimeException("Target group not found"));
        
        List<GroupSymbol> sourceGroupSymbols = groupSymbolRepo.findByGroup(sourceGroup);
        
        for (GroupSymbol sourceGs : sourceGroupSymbols) {
            // Check if target group already has this symbol
            if (groupSymbolRepo.findByGroupAndSymbol(targetGroup, sourceGs.getSymbol()).isEmpty()) {
                GroupSymbol newGs = new GroupSymbol();
                newGs.setGroup(targetGroup);
                newGs.setSymbol(sourceGs.getSymbol());
                newGs.setCanViewQuotes(sourceGs.isCanViewQuotes());
                newGs.setCanTrade(sourceGs.isCanTrade());
                newGs.setBidMarkup(sourceGs.getBidMarkup());
                newGs.setAskMarkup(sourceGs.getAskMarkup());
                newGs.setBidMarkupPercent(sourceGs.getBidMarkupPercent());
                newGs.setAskMarkupPercent(sourceGs.getAskMarkupPercent());
                
                groupSymbolRepo.save(newGs);
            }
        }
    }

    public Map<String, Object> getGroupDashboardData() {
        List<Group> groups = groupRepo.findAll();
        
        Map<String, Long> groupMemberCounts = groups.stream()
                .collect(Collectors.toMap(
                    Group::getName,
                    group -> (long) accountRepo.findByGroup(group).size()
                ));
        
        Map<String, Long> groupSymbolCounts = groups.stream()
                .collect(Collectors.toMap(
                    Group::getName,
                    group -> (long) groupSymbolRepo.findByGroup(group).size()
                ));
        
        return Map.of(
            "groups", groups,
            "memberCounts", groupMemberCounts,
            "symbolCounts", groupSymbolCounts,
            "stats", getGroupStatistics()
        );
    }

    public void bulkAssignUsersToGroup(Long groupId, List<Long> accountIds) {
        Group group = groupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        List<Account> accounts = accountRepo.findAllById(accountIds);
        
        for (Account account : accounts) {
            if (!account.isAdmin()) { // Don't assign admin accounts to groups
                account.setGroup(group);
                accountRepo.save(account);
            }
        }
    }

    public void bulkRemoveUsersFromGroup(Long groupId, List<Long> accountIds) {
        List<Account> accounts = accountRepo.findAllById(accountIds);
        
        for (Account account : accounts) {
            if (account.getGroup() != null && account.getGroup().getId().equals(groupId)) {
                account.setGroup(null);
                accountRepo.save(account);
                
                // Optionally clear their watchlist when removed from group
                clearAccountWatchlist(account);
            }
        }
    }

    private void clearAccountWatchlist(Account account) {
        List<Watchlist> watchlistItems = watchlistRepo.findByAccountOrderBySortOrder(account);
        watchlistRepo.deleteAll(watchlistItems);
    }

    public List<Symbol> getSymbolsNotInGroup(Long groupId) {
        Group group = groupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        List<Symbol> allSymbols = symbolRepo.findByActiveTrue();
        List<GroupSymbol> groupSymbols = groupSymbolRepo.findByGroup(group);
        List<Long> assignedSymbolIds = groupSymbols.stream()
                .map(gs -> gs.getSymbol().getId())
                .collect(Collectors.toList());
        
        return allSymbols.stream()
                .filter(symbol -> !assignedSymbolIds.contains(symbol.getId()))
                .collect(Collectors.toList());
    }

    public void updateGroupMarkups(Long groupId, double bidMarkupPercent, double askMarkupPercent) {
        Group group = groupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        List<GroupSymbol> groupSymbols = groupSymbolRepo.findByGroup(group);
        
        for (GroupSymbol gs : groupSymbols) {
            gs.setBidMarkupPercent(bidMarkupPercent);
            gs.setAskMarkupPercent(askMarkupPercent);
            groupSymbolRepo.save(gs);
        }
    }
}