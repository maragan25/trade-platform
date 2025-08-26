package yes.example.no.controller;

import yes.example.no.entity.*;
import yes.example.no.repository.*;
import yes.example.no.service.AdminValidationService;
import yes.example.no.service.GroupManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AccountRepository accountRepo;
    private final GroupRepository groupRepo;
    private final SymbolRepository symbolRepo;
    private final TradeRepository tradeRepo;
    private final GroupManagementService groupManagementService;
    private final AdminValidationService adminValidationService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    private void notifyGroupChange(Long groupId, String changeType, String message) {
    Optional<Group> groupOpt = groupRepo.findById(groupId);
    if (groupOpt.isPresent()) {
        List<Account> groupMembers = accountRepo.findByGroup(groupOpt.get());
        List<String> usernames = groupMembers.stream()
                .map(Account::getUsername)
                .collect(Collectors.toList());
        
        Map<String, Object> notification = Map.of(
            "type", changeType,
            "groupId", groupId,
            "groupName", groupOpt.get().getName(),
            "message", message,
            "timestamp", System.currentTimeMillis()
        );
        
        // Broadcast to group members
        usernames.forEach(username -> {
            messagingTemplate.convertAndSendToUser(username, "/queue/notifications", notification);
        });
        
        // Broadcast to all admins
        messagingTemplate.convertAndSend("/topic/group-updates", notification);
    }
}

    public static class DashboardStats {
        public long totalAccounts;
        public long totalGroups;
        public long totalSymbols;
        public long activeTrades;
        public double totalBalance;
        public double totalEquity;
        public long unassignedUsers;

        public DashboardStats(long totalAccounts, long totalGroups, long totalSymbols, 
                             long activeTrades, double totalBalance, double totalEquity, long unassignedUsers) {
            this.totalAccounts = totalAccounts;
            this.totalGroups = totalGroups;
            this.totalSymbols = totalSymbols;
            this.activeTrades = activeTrades;
            this.totalBalance = totalBalance;
            this.totalEquity = totalEquity;
            this.unassignedUsers = unassignedUsers;
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getDashboardStats(HttpServletRequest request) {
                adminValidationService.validateAdmin(request);

        List<Account> allAccounts = accountRepo.findAll();
        List<Account> nonAdminAccounts = allAccounts.stream()
                .filter(acc -> !acc.isAdmin())
                .toList();

        long totalAccounts = nonAdminAccounts.size();
        long totalGroups = groupRepo.count();
        long totalSymbols = symbolRepo.count();
        long activeTrades = tradeRepo.findAll().stream()
                .filter(Trade::isOpen)
                .count();

        double totalBalance = nonAdminAccounts.stream()
                .mapToDouble(Account::getBalance)
                .sum();

        double totalEquity = nonAdminAccounts.stream()
                .mapToDouble(Account::getEquity)
                .sum();

        long unassignedUsers = nonAdminAccounts.stream()
                .filter(acc -> acc.getGroup() == null)
                .count();

        DashboardStats stats = new DashboardStats(
            totalAccounts, totalGroups, totalSymbols, activeTrades, 
            totalBalance, totalEquity, unassignedUsers
        );

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getDashboardOverview(HttpServletRequest request) {
                adminValidationService.validateAdmin(request);
        return ResponseEntity.ok(groupManagementService.getGroupDashboardData());
    }

    @GetMapping("/recent-activities")
    public ResponseEntity<List<Object>> getRecentActivities(HttpServletRequest request) {
                adminValidationService.validateAdmin(request);

        List<Trade> recentTrades = tradeRepo.findAll().stream()
                .filter(trade -> trade.getOpenTime().isAfter(
                    java.time.LocalDateTime.now().minusDays(7)
                ))
                .sorted((t1, t2) -> t2.getOpenTime().compareTo(t1.getOpenTime()))
                .limit(10)
                .toList();

        List<Object> activities = recentTrades.stream()
                .map(trade -> Map.of(
                    "type", "TRADE",
                    "description", trade.getAccount().getUsername() + " " + 
                                  trade.getType().toLowerCase() + " " + 
                                  trade.getSize() + " " + trade.getSymbol().getName(),
                    "timestamp", trade.getOpenTime(),
                    "status", trade.isOpen() ? "OPEN" : "CLOSED"
                ))
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(activities);
    }

    @PostMapping("/groups/{groupId}/clone-to/{targetGroupId}")
    public ResponseEntity<Void> cloneGroupConfiguration(@PathVariable Long groupId, 
                                                        @PathVariable Long targetGroupId, 
                                                        HttpServletRequest request) {
                adminValidationService.validateAdmin(request);
        groupManagementService.cloneGroupConfiguration(groupId, targetGroupId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/groups/{groupId}/bulk-assign-users")
    public ResponseEntity<Void> bulkAssignUsersToGroup(@PathVariable Long groupId, 
                                                       @RequestBody List<Long> accountIds, 
                                                       HttpServletRequest request) {
                adminValidationService.validateAdmin(request);
        groupManagementService.bulkAssignUsersToGroup(groupId, accountIds);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/groups/{groupId}/bulk-remove-users")
    public ResponseEntity<Void> bulkRemoveUsersFromGroup(@PathVariable Long groupId, 
                                                         @RequestBody List<Long> accountIds, 
                                                         HttpServletRequest request) {
                adminValidationService.validateAdmin(request);
        groupManagementService.bulkRemoveUsersFromGroup(groupId, accountIds);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/groups/{groupId}/update-markups")
    public ResponseEntity<Void> updateGroupMarkups(@PathVariable Long groupId,
                                                   @RequestParam double bidMarkupPercent,
                                                   @RequestParam double askMarkupPercent,
                                                   HttpServletRequest request) {
                adminValidationService.validateAdmin(request);
        groupManagementService.updateGroupMarkups(groupId, bidMarkupPercent, askMarkupPercent);
        return ResponseEntity.ok().build();
    }
}
