package yes.example.no.controller;

import yes.example.no.dto.SharedDTOs.*;

import yes.example.no.entity.*;
import yes.example.no.repository.*;
import yes.example.no.service.AdminValidationService;
import yes.example.no.service.WebSocketNotificationService;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import yes.example.no.service.AdminGroupService;

import jakarta.servlet.http.HttpServletRequest;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

import java.util.HashMap;
@RestController
@RequestMapping("/api/admin/groups")
@RequiredArgsConstructor
public class AdminGroupController {

    private final GroupRepository groupRepo;
    private final GroupSymbolRepository groupSymbolRepo;
    private final SymbolRepository symbolRepo;
    private final AccountRepository accountRepo;
    private final SimpMessagingTemplate messagingTemplate;  
    private final AdminValidationService adminValidationService;
        @Autowired
    private WebSocketNotificationService notificationService;
        @Autowired
    private AdminGroupService adminGroupService;

    public List<GroupSymbol> getGroupSymbols(Long groupId) {
        Group group = groupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        return groupSymbolRepo.findByGroup(group);
    }

    public List<Symbol> getAvailableSymbolsForGroup(Long groupId) {
        Group group = groupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        List<GroupSymbol> groupSymbols = groupSymbolRepo.findByGroup(group);
        List<Long> assignedSymbolIds = groupSymbols.stream()
                .map(gs -> gs.getSymbol().getId())
                .collect(Collectors.toList());
        
        List<Symbol> allSymbols = symbolRepo.findByActiveTrue();
        return allSymbols.stream()
                .filter(symbol -> !assignedSymbolIds.contains(symbol.getId()))
                .collect(Collectors.toList());
    }

    @Transactional
    public GroupSymbol updateGroupSymbol(Long groupId, Long groupSymbolId, GroupSymbol updatedGroupSymbol) {
        Group group = groupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        List<GroupSymbol> groupSymbols = groupSymbolRepo.findByGroup(group);
        GroupSymbol existingGroupSymbol = groupSymbols.stream()
                .filter(gs -> gs.getId().equals(groupSymbolId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Group symbol not found"));
        
        // Update the fields
        existingGroupSymbol.setCanViewQuotes(updatedGroupSymbol.isCanViewQuotes());
        existingGroupSymbol.setCanTrade(updatedGroupSymbol.isCanTrade());
        existingGroupSymbol.setBidMarkup(updatedGroupSymbol.getBidMarkup());
        existingGroupSymbol.setAskMarkup(updatedGroupSymbol.getAskMarkup());
        existingGroupSymbol.setBidMarkupPercent(updatedGroupSymbol.getBidMarkupPercent());
        existingGroupSymbol.setAskMarkupPercent(updatedGroupSymbol.getAskMarkupPercent());
        
        GroupSymbol saved = groupSymbolRepo.save(existingGroupSymbol);
        
        // Notify group members about the change
        notificationService.notifyGroupMembersSymbolsChanged(groupId, 
            "Symbol access permissions have been updated for " + saved.getSymbol().getName());
        
        return saved;
    }

    @Transactional
    public void removeSymbolFromGroup(Long groupId, Long symbolId) {
        Group group = groupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        Symbol symbol = symbolRepo.findById(symbolId)
                .orElseThrow(() -> new RuntimeException("Symbol not found"));
        
        groupSymbolRepo.deleteByGroupAndSymbol(group, symbol);
        
        // Notify group members
        notificationService.notifyGroupMembersSymbolsChanged(groupId, 
            "Symbol " + symbol.getName() + " has been removed from your group");
    }

    @Transactional
    public GroupSymbol addSymbolToGroup(Long groupId, Long symbolId, GroupSymbol config) {
        Group group = groupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        Symbol symbol = symbolRepo.findById(symbolId)
                .orElseThrow(() -> new RuntimeException("Symbol not found"));
        
        // Check if already exists
        Optional<GroupSymbol> existing = groupSymbolRepo.findByGroupAndSymbol(group, symbol);
        if (existing.isPresent()) {
            throw new RuntimeException("Symbol already assigned to group");
        }
        
        GroupSymbol groupSymbol = new GroupSymbol();
        groupSymbol.setGroup(group);
        groupSymbol.setSymbol(symbol);
        groupSymbol.setCanViewQuotes(config.isCanViewQuotes());
        groupSymbol.setCanTrade(config.isCanTrade());
        groupSymbol.setBidMarkup(config.getBidMarkup());
        groupSymbol.setAskMarkup(config.getAskMarkup());
        groupSymbol.setBidMarkupPercent(config.getBidMarkupPercent());
        groupSymbol.setAskMarkupPercent(config.getAskMarkupPercent());
        
        GroupSymbol saved = groupSymbolRepo.save(groupSymbol);
        
        // Notify group members
        notificationService.notifyGroupMembersSymbolsChanged(groupId, 
            "New symbol " + symbol.getName() + " has been added to your group");
        
        return saved;
    }

    public boolean canAccountAccessSymbol(Long accountId, Long symbolId) {
        Account account = accountRepo.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        if (account.getGroup() == null) {
            return false;
        }
        
        List<GroupSymbol> groupSymbols = groupSymbolRepo.findByGroup(account.getGroup());
        return groupSymbols.stream()
                .anyMatch(gs -> gs.getSymbol().getId().equals(symbolId) && gs.isCanViewQuotes());
    }

    public boolean canAccountTradeSymbol(Long accountId, Long symbolId) {
        Account account = accountRepo.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        if (account.getGroup() == null) {
            return false;
        }
        
        List<GroupSymbol> groupSymbols = groupSymbolRepo.findByGroup(account.getGroup());
        return groupSymbols.stream()
                .anyMatch(gs -> gs.getSymbol().getId().equals(symbolId) && gs.isCanTrade());
    }

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

    private void validateAdmin(Principal principal, HttpServletRequest request) {
        adminValidationService.validateAdmin(request);
    }

    @PostMapping("/{groupId}/symbols/bulk")
    public ResponseEntity<?> addSymbolsToGroupBulk(
            @PathVariable Long groupId,
            @RequestBody List<Map<String, Object>> configs,
            Principal principal,
            HttpServletRequest request) {
    
        try {
            adminValidationService.validateAdmin(request);
    
            Group group = groupRepo.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Group not found"));
    
            List<String> addedSymbols = new ArrayList<>();
            List<String> skippedSymbols = new ArrayList<>();
    
            for (Map<String, Object> config : configs) {
                try {
                    Long symbolId = Long.parseLong(config.get("symbolId").toString());
    
                    Symbol symbol = symbolRepo.findById(symbolId)
                            .orElseThrow(() -> new RuntimeException("Symbol with ID " + symbolId + " not found"));
    
                    Optional<GroupSymbol> existing = groupSymbolRepo.findByGroupAndSymbol(group, symbol);
    
                    if (existing.isPresent()) {
                        skippedSymbols.add(symbol.getName() + " (already assigned)");
                        continue; // skip duplicates
                    }
    
                    GroupSymbol groupSymbol = new GroupSymbol();
                    groupSymbol.setGroup(group);
                    groupSymbol.setSymbol(symbol);
    
                    // Safe Boolean conversion
                    groupSymbol.setCanViewQuotes(Boolean.parseBoolean(
                            config.getOrDefault("canViewQuotes", true).toString()
                    ));
                    groupSymbol.setCanTrade(Boolean.parseBoolean(
                            config.getOrDefault("canTrade", true).toString()
                    ));
    
                    // Safe numeric conversion
                    groupSymbol.setBidMarkup(Double.parseDouble(
                            config.getOrDefault("bidMarkup", 0.0).toString()
                    ));
                    groupSymbol.setAskMarkup(Double.parseDouble(
                            config.getOrDefault("askMarkup", 0.0).toString()
                    ));
                    groupSymbol.setBidMarkupPercent(Double.parseDouble(
                            config.getOrDefault("bidMarkupPercent", 0.0).toString()
                    ));
                    groupSymbol.setAskMarkupPercent(Double.parseDouble(
                            config.getOrDefault("askMarkupPercent", 0.0).toString()
                    ));
    
                    groupSymbolRepo.save(groupSymbol);
                    addedSymbols.add(symbol.getName());
    
                } catch (Exception innerEx) {
                    // Skip problematic symbol but log it
                    innerEx.printStackTrace();
                    skippedSymbols.add(config.get("symbolId") + " (error: " + innerEx.getMessage() + ")");
                }
            }
    
            Map<String, Object> response = new HashMap<>();
            response.put("addedSymbols", addedSymbols);
            response.put("skippedSymbols", skippedSymbols);
            response.put("totalAdded", addedSymbols.size());
            response.put("totalSkipped", skippedSymbols.size());
    
            // Notify group members with updated permissions map
            if (!addedSymbols.isEmpty()) {
                List<GroupSymbol> groupSymbols = groupSymbolRepo.findByGroup(group);

                List<Map<String, Object>> permissions = groupSymbols.stream()
                .map(gs -> (Map<String, Object>) (Map<?, ?>) Map.of(
                    "symbolId", gs.getSymbol().getId(),
                    "canTrade", gs.isCanTrade(),
                    "canViewQuotes", gs.isCanViewQuotes()
                ))
                .collect(Collectors.toList());
            

                notificationService.notifyGroupPermissionsUpdate(groupId, permissions);
            }
    
            return ResponseEntity.ok(response);
    
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error adding symbols: " + e.getMessage());
        }
    }
    

    @GetMapping
    public ResponseEntity<List<GroupDetailDto>> getAllGroups(HttpServletRequest request) {
        try {
                    adminValidationService.validateAdmin(request); 

            
            List<Group> groups = groupRepo.findAll();
            List<GroupDetailDto> groupDetails = groups.stream()
                    .map(group -> {
                        int memberCount = accountRepo.findByGroup(group).size();
                        int symbolCount = groupSymbolRepo.findByGroup(group).size();
                        return new GroupDetailDto(group, memberCount, symbolCount);
                    })
                    .collect(Collectors.toList());
                    
            return ResponseEntity.ok(groupDetails);
        } catch (RuntimeException e) {
            System.err.println("Error in getAllGroups: " + e.getMessage());
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            System.err.println("Unexpected error in getAllGroups: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

        @GetMapping("/{groupId}/members")
    public ResponseEntity<?> getGroupMembers(@PathVariable Long groupId, HttpServletRequest request) {
        try {
            adminValidationService.validateAdmin(request);            
            Group group = groupRepo.findById(groupId).orElse(null);
            if (group == null) {
                return ResponseEntity.notFound().build();
            }
            
            List<Account> members = new ArrayList<>(group.getAccounts());
            return ResponseEntity.ok(members);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/unassigned-accounts")
    public ResponseEntity<?> getUnassignedAccounts(HttpServletRequest request) {
        try {
            adminValidationService.validateAdmin(request);            
            List<Account> unassignedAccounts = accountRepo.findByGroupIsNullAndIsAdminFalse();
            return ResponseEntity.ok(unassignedAccounts);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<GroupDetailDto> createGroup(@RequestBody CreateGroupRequest grouprequest, HttpServletRequest request) {
        try {
                    adminValidationService.validateAdmin(request); 

            
            if (groupRepo.findByName(grouprequest.getName()).isPresent()) {
                throw new RuntimeException("Group name '" + grouprequest.getName() + "' already exists");
            }
            
            Group group = new Group();
            group.setName(grouprequest.getName());
            group.setDescription(grouprequest.getDescription());
            group.setActive(grouprequest.isActive());
            
            Group savedGroup = groupRepo.save(group);
            GroupDetailDto result = new GroupDetailDto(savedGroup, 0, 0);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            System.err.println("Error creating group: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{groupId}/symbols/{groupSymbolId}")
    public ResponseEntity<GroupSymbol> getGroupSymbol(@PathVariable Long groupId, 
                                                      @PathVariable Long groupSymbolId,
                                                      Principal principal, 
                                                      HttpServletRequest request) {
        adminValidationService.validateAdmin(request);
        
        GroupSymbol groupSymbol = adminGroupService.getGroupSymbols(groupId)
                .stream()
                .filter(gs -> gs.getId().equals(groupSymbolId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Group symbol not found"));
        
        return ResponseEntity.ok(groupSymbol);
    }

    @GetMapping("/{groupId}/symbols")
    public ResponseEntity<List<GroupSymbolDto>> getGroupSymbols(
            @PathVariable Long groupId,
            HttpServletRequest request) {
        
        adminValidationService.validateAdmin(request);
        
        Optional<Group> groupOpt = groupRepo.findById(groupId);
        if (groupOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Group group = groupOpt.get();
        List<GroupSymbol> groupSymbols = groupSymbolRepo.findByGroup(group);
        
        List<GroupSymbolDto> dtos = groupSymbols.stream()
            .map(gs -> new GroupSymbolDto(
                gs.getId(),
                new SymbolDto(gs.getSymbol().getId(), gs.getSymbol().getName(), 
                             gs.getSymbol().getBidPrice(), gs.getSymbol().getAskPrice(), 
                             gs.getSymbol().isActive()),
                gs.isCanViewQuotes(),
                gs.isCanTrade(),
                gs.getBidMarkup(),
                gs.getAskMarkup(),
                gs.getBidMarkupPercent(),
                gs.getAskMarkupPercent()
            ))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{groupId}/available-symbols")
    public ResponseEntity<List<SymbolDto>> getAvailableSymbols(@PathVariable Long groupId, Principal principal, HttpServletRequest request) {
        validateAdmin(principal, request);        
        try {
            List<Symbol> availableSymbols = adminGroupService.getAvailableSymbolsForGroup(groupId);
            List<SymbolDto> symbolDtos = availableSymbols.stream()
                    .map(SymbolDto::new)  // Use the constructor instead of manual creation
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(symbolDtos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<GroupDetailDto> updateGroup(@PathVariable Long groupId, 
                                                 @RequestBody CreateGroupRequest grouprequest, 
                                                 HttpServletRequest request) {
    try {
                adminValidationService.validateAdmin(request); 

        
        Group group = groupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        // Check if name is being changed and if new name already exists
        if (!group.getName().equals(grouprequest.getName())) {
            if (groupRepo.findByName(grouprequest.getName()).isPresent()) {
                throw new RuntimeException("Group name '" + grouprequest.getName() + "' already exists");
            }
        }
        
        group.setName(grouprequest.getName());
        group.setDescription(grouprequest.getDescription());
        group.setActive(grouprequest.isActive());
        
        Group savedGroup = groupRepo.save(group);
        
        // Get current counts
        int memberCount = accountRepo.findByGroup(savedGroup).size();
        int symbolCount = groupSymbolRepo.findByGroup(savedGroup).size();
        
        // Notify affected users about group changes
        List<Account> members = accountRepo.findByGroup(savedGroup);
        for (Account member : members) {
            Map<String, Object> userNotification = new HashMap<>();
            userNotification.put("type", "GROUP_CHANGED");
            userNotification.put("message", "Your group settings have been updated");
            userNotification.put("groupName", savedGroup.getName());
            userNotification.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSendToUser(
                member.getUsername(),
                "/queue/account",
                userNotification
            );
        }
        
        GroupDetailDto result = new GroupDetailDto(savedGroup, memberCount, symbolCount);
        return ResponseEntity.ok(result);
    } catch (RuntimeException e) {
        System.err.println("Error updating group: " + e.getMessage());
        return ResponseEntity.badRequest().build();
    }
}

    @PutMapping("/{groupId}/symbols/{groupSymbolId}")
    public ResponseEntity<GroupSymbol> updateGroupSymbol(@PathVariable Long groupId,
                                                     @PathVariable Long groupSymbolId,
                                                     @RequestBody GroupSymbol groupSymbol,
                                                     Principal principal,
                                                     HttpServletRequest request) {
    adminValidationService.validateAdmin(request);
    
    GroupSymbol updated = adminGroupService.updateGroupSymbol(groupId, groupSymbolId, groupSymbol);
    
        // Notify clients of changes
    // Rebuild permissions map after update
    List<GroupSymbol> groupSymbols = groupSymbolRepo.findByGroup(updated.getGroup());
    List<Map<String, Object>> permissions = groupSymbols.stream()
            .map(gs -> (Map<String, Object>) (Map<?, ?>) Map.of(
                "symbolId", gs.getSymbol().getId(),
                "canTrade", gs.isCanTrade(),
                "canViewQuotes", gs.isCanViewQuotes()
            ))
            .collect(Collectors.toList());


    notificationService.notifyGroupPermissionsUpdate(groupId, permissions);

    return ResponseEntity.ok(updated);
}

    @DeleteMapping("/{groupId}/symbols/{symbolId}")
    public ResponseEntity<Void> removeSymbolFromGroup(@PathVariable Long groupId, 
                                                     @PathVariable Long symbolId, 
                                                     HttpServletRequest request) {
        try {
                    adminValidationService.validateAdmin(request); 

            
            Group group = groupRepo.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Group not found"));
            Symbol symbol = symbolRepo.findById(symbolId)
                    .orElseThrow(() -> new RuntimeException("Symbol not found"));
            
            groupSymbolRepo.deleteByGroupAndSymbol(group, symbol);

            Map<String, Object> removalMessage = new HashMap<>();
            removalMessage.put("type", "SYMBOL_ACCESS_CHANGED");
            removalMessage.put("groupId", groupId);
            removalMessage.put("symbolId", symbolId);
            removalMessage.put("message", "Symbol removed from group");
            removalMessage.put("timestamp", System.currentTimeMillis());
            messagingTemplate.convertAndSend("/topic/admin-messages", removalMessage);

            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            System.err.println("Error removing symbol from group: " + e.getMessage());
            return ResponseEntity.status(403).build();
        }
    }

    @PostMapping("/{groupId}/members/{accountId}")
    public ResponseEntity<AccountDto> assignAccountToGroup(@PathVariable Long groupId, 
                                                          @PathVariable Long accountId, 
                                                          HttpServletRequest request) {
        try {
                    adminValidationService.validateAdmin(request); 

            
            Group group = groupRepo.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Group not found"));
            Account account = accountRepo.findById(accountId)
                    .orElseThrow(() -> new RuntimeException("Account not found"));
            
            if (account.isAdmin()) {
                throw new RuntimeException("Cannot assign admin accounts to groups");
            }
            
            account.setGroup(group);
            Account saved = accountRepo.save(account);
            return ResponseEntity.ok(new AccountDto(saved));
        } catch (RuntimeException e) {
            System.err.println("Error assigning account to group: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<String> removeUserFromGroup(
            @PathVariable Long groupId,
            @PathVariable Long userId,
            HttpServletRequest request) {
        
        adminValidationService.validateAdmin(request);
        
        Optional<Group> groupOpt = groupRepo.findById(groupId);
        Optional<Account> accountOpt = accountRepo.findById(userId);
        
        if (groupOpt.isEmpty() || accountOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Account account = accountOpt.get();
        account.setGroup(null);
        accountRepo.save(account);
        
        return ResponseEntity.ok("User removed from group successfully");
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long groupId, HttpServletRequest request) {
        try {
                    adminValidationService.validateAdmin(request); 

            
            Group group = groupRepo.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Group not found"));
            
            List<Account> members = accountRepo.findByGroup(group);
            if (!members.isEmpty()) {
                throw new RuntimeException("Cannot delete group with members. Remove all members first.");
            }
            
            List<GroupSymbol> groupSymbols = groupSymbolRepo.findByGroup(group);
            groupSymbolRepo.deleteAll(groupSymbols);
            groupRepo.delete(group);
            
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            System.err.println("Error deleting group: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    public void notifyUserGroupChange(Long userId, String newGroupName) {
        Account user = accountRepo.findById(userId).orElse(null);
        if (user != null) {
            Map<String, Object> userNotification = new HashMap<>();
            userNotification.put("type", "USER_GROUP_CHANGED");
            userNotification.put("message", "You have been assigned to group: " + newGroupName);
            userNotification.put("groupName", newGroupName);
            userNotification.put("timestamp", System.currentTimeMillis());
            
            // Send to specific user
            messagingTemplate.convertAndSendToUser(
                user.getUsername(),
                "/queue/account",
                userNotification
            );
        }
    }

    @PostMapping("/{groupId}/bulk-assign-users")
    public ResponseEntity<List<AccountDto>> bulkAssignUsersToGroup(@PathVariable Long groupId,
                                                              @RequestBody List<Long> userIds,
                                                              HttpServletRequest request) {
    try {
                adminValidationService.validateAdmin(request); 

        
        Group group = groupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        List<Account> assignedAccounts = new ArrayList<>();
        
        for (Long userId : userIds) {
            Account account = accountRepo.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Account not found: " + userId));
            
            if (account.isAdmin()) {
                throw new RuntimeException("Cannot assign admin accounts to groups");
            }
            
            account.setGroup(group);
            Account saved = accountRepo.save(account);
            assignedAccounts.add(saved);
            
            // Notify user about group assignment
            Map<String, Object> userNotification = new HashMap<>();
            userNotification.put("type", "GROUP_CHANGED");
            userNotification.put("message", "You have been assigned to group: " + group.getName());
            userNotification.put("groupName", group.getName());
            userNotification.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSendToUser(
                account.getUsername(),
                "/queue/account",
                userNotification
            );
        }
        
        List<AccountDto> result = assignedAccounts.stream()
                .map(AccountDto::new)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(result);
    } catch (RuntimeException e) {
        System.err.println("Error bulk assigning users: " + e.getMessage());
        return ResponseEntity.badRequest().build();
    }
}

    @PostMapping("/broadcast-group-change")
    public ResponseEntity<String> broadcastGroupChange(
            @RequestParam Long groupId,
            @RequestParam String changeType,
            HttpServletRequest request) {
        
        adminValidationService.validateAdmin(request);
        
        // Broadcast to all users in the affected group
        List<Account> groupMembers = accountRepo.findByGroupId(groupId);
        for (Account member : groupMembers) {
            Map<String, Object> notification = Map.of(
                "type", changeType,
                "message", "Your group access has been updated",
                "groupId", groupId
            );
            
            messagingTemplate.convertAndSendToUser(
                member.getUsername(), 
                "/queue/notifications", 
                notification
            );
        }
    
    return ResponseEntity.ok("Notification sent");
}
}