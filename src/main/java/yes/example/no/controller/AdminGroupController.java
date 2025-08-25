package yes.example.no.controller;

import yes.example.no.entity.*;
import yes.example.no.repository.*;
import yes.example.no.service.AdminValidationService;
import yes.example.no.service.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Map;
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
 

    // DTO Classes to avoid circular references
    public static class GroupDetailDto {
        private Long id;
        private String name;
        private String description;
        private boolean active;
        private int memberCount;
        private int symbolCount;
        
        public GroupDetailDto(Group group, int memberCount, int symbolCount) {
            this.id = group.getId();
            this.name = group.getName();
            this.description = group.getDescription();
            this.active = group.isActive();
            this.memberCount = memberCount;
            this.symbolCount = symbolCount;
        }
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public int getMemberCount() { return memberCount; }
        public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
        public int getSymbolCount() { return symbolCount; }
        public void setSymbolCount(int symbolCount) { this.symbolCount = symbolCount; }
    }

    public static class GroupSymbolDto {
        private Long id;
        private Long groupId;
        private String groupName;
        private SymbolDto symbol;
        private boolean canViewQuotes;
        private boolean canTrade;
        private double bidMarkup;
        private double askMarkup;
        private double bidMarkupPercent;
        private double askMarkupPercent;
        
        public GroupSymbolDto(GroupSymbol gs) {
            this.id = gs.getId();
            this.groupId = gs.getGroup().getId();
            this.groupName = gs.getGroup().getName();
            this.symbol = new SymbolDto(gs.getSymbol());
            this.canViewQuotes = gs.isCanViewQuotes();
            this.canTrade = gs.isCanTrade();
            this.bidMarkup = gs.getBidMarkup();
            this.askMarkup = gs.getAskMarkup();
            this.bidMarkupPercent = gs.getBidMarkupPercent();
            this.askMarkupPercent = gs.getAskMarkupPercent();
        }
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getGroupId() { return groupId; }
        public void setGroupId(Long groupId) { this.groupId = groupId; }
        public String getGroupName() { return groupName; }
        public void setGroupName(String groupName) { this.groupName = groupName; }
        public SymbolDto getSymbol() { return symbol; }
        public void setSymbol(SymbolDto symbol) { this.symbol = symbol; }
        public boolean isCanViewQuotes() { return canViewQuotes; }
        public void setCanViewQuotes(boolean canViewQuotes) { this.canViewQuotes = canViewQuotes; }
        public boolean isCanTrade() { return canTrade; }
        public void setCanTrade(boolean canTrade) { this.canTrade = canTrade; }
        public double getBidMarkup() { return bidMarkup; }
        public void setBidMarkup(double bidMarkup) { this.bidMarkup = bidMarkup; }
        public double getAskMarkup() { return askMarkup; }
        public void setAskMarkup(double askMarkup) { this.askMarkup = askMarkup; }
        public double getBidMarkupPercent() { return bidMarkupPercent; }
        public void setBidMarkupPercent(double bidMarkupPercent) { this.bidMarkupPercent = bidMarkupPercent; }
        public double getAskMarkupPercent() { return askMarkupPercent; }
        public void setAskMarkupPercent(double askMarkupPercent) { this.askMarkupPercent = askMarkupPercent; }
    }

    public static class SymbolDto {
        private Long id;
        private String name;
        private double bidPrice;
        private double askPrice;
        private boolean active;
        
        public SymbolDto(Symbol symbol) {
            this.id = symbol.getId();
            this.name = symbol.getName();
            this.bidPrice = symbol.getBidPrice();
            this.askPrice = symbol.getAskPrice();
            this.active = symbol.isActive();
        }
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public double getBidPrice() { return bidPrice; }
        public void setBidPrice(double bidPrice) { this.bidPrice = bidPrice; }
        public double getAskPrice() { return askPrice; }
        public void setAskPrice(double askPrice) { this.askPrice = askPrice; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    public static class AccountDto {
        private Long id;
        private String username;
        private double balance;
        private double equity;
        private double marginUsed;
        private double freeMargin;
        private boolean admin;
        private String groupName;
        
        public AccountDto(Account account) {
            this.id = account.getId();
            this.username = account.getUsername();
            this.balance = account.getBalance();
            this.equity = account.getEquity();
            this.marginUsed = account.getMarginUsed();
            this.freeMargin = account.getFreeMargin();
            this.admin = account.isAdmin();
            this.groupName = account.getGroup() != null ? account.getGroup().getName() : null;
        }
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public double getBalance() { return balance; }
        public void setBalance(double balance) { this.balance = balance; }
        public double getEquity() { return equity; }
        public void setEquity(double equity) { this.equity = equity; }
        public double getMarginUsed() { return marginUsed; }
        public void setMarginUsed(double marginUsed) { this.marginUsed = marginUsed; }
        public double getFreeMargin() { return freeMargin; }
        public void setFreeMargin(double freeMargin) { this.freeMargin = freeMargin; }
        public boolean isAdmin() { return admin; }
        public void setAdmin(boolean admin) { this.admin = admin; }
        public String getGroupName() { return groupName; }
        public void setGroupName(String groupName) { this.groupName = groupName; }
    }

    public static class CreateGroupRequest {
        private String name;
        private String description;
        private boolean active = true;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    public static class GroupSymbolConfigDto {
        private Long symbolId;
        private boolean canViewQuotes = true;
        private boolean canTrade = true;
        private double bidMarkup = 0.0;
        private double askMarkup = 0.0;
        private double bidMarkupPercent = 0.0;
        private double askMarkupPercent = 0.0;
        
        // Getters and setters
        public Long getSymbolId() { return symbolId; }
        public void setSymbolId(Long symbolId) { this.symbolId = symbolId; }
        public boolean isCanViewQuotes() { return canViewQuotes; }
        public void setCanViewQuotes(boolean canViewQuotes) { this.canViewQuotes = canViewQuotes; }
        public boolean isCanTrade() { return canTrade; }
        public void setCanTrade(boolean canTrade) { this.canTrade = canTrade; }
        public double getBidMarkup() { return bidMarkup; }
        public void setBidMarkup(double bidMarkup) { this.bidMarkup = bidMarkup; }
        public double getAskMarkup() { return askMarkup; }
        public void setAskMarkup(double askMarkup) { this.askMarkup = askMarkup; }
        public double getBidMarkupPercent() { return bidMarkupPercent; }
        public void setBidMarkupPercent(double bidMarkupPercent) { this.bidMarkupPercent = bidMarkupPercent; }
        public double getAskMarkupPercent() { return askMarkupPercent; }
        public void setAskMarkupPercent(double askMarkupPercent) { this.askMarkupPercent = askMarkupPercent; }
    }
/* 
    @PutMapping("/{groupId}")
public ResponseEntity<GroupDetailDto> updateGroup(@PathVariable Long groupId, 
                                                 @grouprequestBody CreateGroupgrouprequest grouprequest, 
                                                 HttpServletRequest request) {
    try {
                adminValidationService.validateAdmin(request); 

        
        Group group = groupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        // Check if name is being changed and if new name already exists
        if (!group.getName().equals(grouprequest.getName()) && 
            groupRepo.findByName(grouprequest.getName()).isPresent()) {
            throw new RuntimeException("Group name '" + grouprequest.getName() + "' already exists");
        }
        
        group.setName(grouprequest.getName());
        group.setDescription(grouprequest.getDescription());
        group.setActive(grouprequest.isActive());
        
        Group savedGroup = groupRepo.save(group);
        
        // Get current counts
        int memberCount = accountRepo.findByGroup(group).size();
        int symbolCount = groupSymbolRepo.findByGroup(group).size();
        
        GroupDetailDto result = new GroupDetailDto(savedGroup, memberCount, symbolCount);
        
        // Notify affected users about group changes
        List<Account> members = accountRepo.findByGroup(group);
        for (Account member : members) {
            Map<String, Object> userNotification = new HashMap<>();
            userNotification.put("type", "GROUP_CHANGED");
            userNotification.put("message", "Your group settings have been updated");
            userNotification.put("groupName", group.getName());
            userNotification.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSendToUser(
                member.getUsername(),
                "/queue/account",
                userNotification
            );
        }
        
        return ResponseEntity.ok(result);
    } catch (RuntimeException e) {
        System.err.println("Error updating group: " + e.getMessage());
        return ResponseEntity.badgrouprequest().build();
    }
}

@PutMapping("/{groupId}/symbols/{groupSymbolId}")
public ResponseEntity<GroupSymbolDto> updateGroupSymbol(@PathVariable Long groupId,
                                                       @PathVariable Long groupSymbolId,
                                                       @grouprequestBody GroupSymbolConfigDto config,
                                                       HttpServletRequest request) {
    try {
                adminValidationService.validateAdmin(request); 

        
        Group group = groupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        GroupSymbol groupSymbol = groupSymbolRepo.findById(groupSymbolId)
                .orElseThrow(() -> new RuntimeException("Group symbol not found"));
        
        // Verify that the group symbol belongs to the specified group
        if (!groupSymbol.getGroup().getId().equals(groupId)) {
            throw new RuntimeException("Group symbol does not belong to specified group");
        }
        
        // Update the group symbol configuration
        groupSymbol.setCanViewQuotes(config.isCanViewQuotes());
        groupSymbol.setCanTrade(config.isCanTrade());
        groupSymbol.setBidMarkup(config.getBidMarkup());
        groupSymbol.setAskMarkup(config.getAskMarkup());
        groupSymbol.setBidMarkupPercent(config.getBidMarkupPercent());
        groupSymbol.setAskMarkupPercent(config.getAskMarkupPercent());
        
        GroupSymbol saved = groupSymbolRepo.save(groupSymbol);
        
        // Notify all users in this group about symbol access changes
        List<Account> members = accountRepo.findByGroup(group);
        for (Account member : members) {
            Map<String, Object> symbolNotification = new HashMap<>();
            symbolNotification.put("type", "SYMBOL_ACCESS_CHANGED");
            symbolNotification.put("message", "Symbol access permissions updated");
            symbolNotification.put("symbolId", groupSymbol.getSymbol().getId());
            symbolNotification.put("symbolName", groupSymbol.getSymbol().getName());
            symbolNotification.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSendToUser(
                member.getUsername(),
                "/queue/account",
                symbolNotification
            );
        }
        
        return ResponseEntity.ok(new GroupSymbolDto(saved));
    } catch (RuntimeException e) {
        System.err.println("Error updating group symbol: " + e.getMessage());
        return ResponseEntity.badgrouprequest().build();
    }
}

*/

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
    public ResponseEntity<GroupSymbolDto> getGroupSymbol(@PathVariable Long groupId,
                                                        @PathVariable Long groupSymbolId,
                                                        HttpServletRequest request) {
        try {
                    adminValidationService.validateAdmin(request); 

            
            GroupSymbol groupSymbol = groupSymbolRepo.findById(groupSymbolId)
                    .orElseThrow(() -> new RuntimeException("Group symbol not found"));
            
            if (!groupSymbol.getGroup().getId().equals(groupId)) {
                throw new RuntimeException("Group symbol does not belong to specified group");
            }
            
            return ResponseEntity.ok(new GroupSymbolDto(groupSymbol));
        } catch (RuntimeException e) {
            System.err.println("Error getting group symbol: " + e.getMessage());
            return ResponseEntity.status(403).build();
        }
    }

    @GetMapping("/{groupId}/available-symbols")
    public ResponseEntity<List<SymbolDto>> getAvailableSymbolsForGroup(@PathVariable Long groupId, HttpServletRequest request) {
        try {
                    adminValidationService.validateAdmin(request); 

            
            Group group = groupRepo.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Group not found"));
            
            List<Symbol> allSymbols = symbolRepo.findByActiveTrue();
            List<GroupSymbol> groupSymbols = groupSymbolRepo.findByGroup(group);
            List<Long> assignedSymbolIds = groupSymbols.stream()
                    .map(gs -> gs.getSymbol().getId())
                    .collect(Collectors.toList());
            
            List<SymbolDto> availableSymbols = allSymbols.stream()
                    .filter(symbol -> !assignedSymbolIds.contains(symbol.getId()))
                    .map(SymbolDto::new)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(availableSymbols);
        } catch (RuntimeException e) {
            System.err.println("Error getting available symbols: " + e.getMessage());
            return ResponseEntity.status(403).build();
        }
    }

    @PostMapping("/{groupId}/symbols")
    public ResponseEntity<GroupSymbolDto> addSymbolToGroup(@PathVariable Long groupId, 
                                                          @RequestBody GroupSymbolConfigDto config, 
                                                          HttpServletRequest request) {
        try {
                    adminValidationService.validateAdmin(request); 

            
            Group group = groupRepo.findById(groupId)
                    .orElseThrow(() -> new RuntimeException("Group not found"));
            Symbol symbol = symbolRepo.findById(config.getSymbolId())
                    .orElseThrow(() -> new RuntimeException("Symbol not found"));
            
            if (groupSymbolRepo.findByGroupAndSymbol(group, symbol).isPresent()) {
                throw new RuntimeException("Symbol already assigned to this group");
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

            Map<String, Object> symbolChangeMessage = new HashMap<>();
            symbolChangeMessage.put("type", "SYMBOL_ACCESS_CHANGED");
            symbolChangeMessage.put("groupId", groupId);
            symbolChangeMessage.put("symbolId", config.getSymbolId());
            symbolChangeMessage.put("message", "Symbol access updated for group");
            symbolChangeMessage.put("timestamp", System.currentTimeMillis());
            messagingTemplate.convertAndSend("/topic/admin-messages", symbolChangeMessage);

            return ResponseEntity.ok(new GroupSymbolDto(saved));
        } catch (RuntimeException e) {
            System.err.println("Error adding symbol to group: " + e.getMessage());
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
public ResponseEntity<GroupSymbolDto> updateGroupSymbol(@PathVariable Long groupId,
                                                       @PathVariable Long groupSymbolId,
                                                       @RequestBody GroupSymbolConfigDto config,
                                                       HttpServletRequest request) {
    try {
                adminValidationService.validateAdmin(request); 

        
        Group group = groupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        GroupSymbol groupSymbol = groupSymbolRepo.findById(groupSymbolId)
                .orElseThrow(() -> new RuntimeException("Group symbol not found"));
        
        // Verify the group symbol belongs to the specified group
        if (!groupSymbol.getGroup().getId().equals(groupId)) {
            throw new RuntimeException("Group symbol does not belong to specified group");
        }
        
        // Update the group symbol configuration
        groupSymbol.setCanViewQuotes(config.isCanViewQuotes());
        groupSymbol.setCanTrade(config.isCanTrade());
        groupSymbol.setBidMarkup(config.getBidMarkup());
        groupSymbol.setAskMarkup(config.getAskMarkup());
        groupSymbol.setBidMarkupPercent(config.getBidMarkupPercent());
        groupSymbol.setAskMarkupPercent(config.getAskMarkupPercent());
        
        GroupSymbol saved = groupSymbolRepo.save(groupSymbol);
        
        // Notify affected users about symbol permission changes
        List<Account> members = accountRepo.findByGroup(group);
        for (Account member : members) {
            Map<String, Object> userNotification = new HashMap<>();
            userNotification.put("type", "SYMBOL_ACCESS_CHANGED");
            userNotification.put("message", "Symbol permissions updated");
            userNotification.put("symbolName", groupSymbol.getSymbol().getName());
            userNotification.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSendToUser(
                member.getUsername(),
                "/queue/account",
                userNotification
            );
        }
        
        return ResponseEntity.ok(new GroupSymbolDto(saved));
    } catch (RuntimeException e) {
        System.err.println("Error updating group symbol: " + e.getMessage());
        return ResponseEntity.badRequest().build();
    }
}

@PostMapping("/{groupId}/symbols/bulk")
public ResponseEntity<List<GroupSymbolDto>> addMultipleSymbolsToGroup(@PathVariable Long groupId,
                                                                      @RequestBody List<GroupSymbolConfigDto> configs,
                                                                      HttpServletRequest request) {
    try {
                adminValidationService.validateAdmin(request); 

        
        Group group = groupRepo.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        
        List<GroupSymbol> savedSymbols = new ArrayList<>();
        
        for (GroupSymbolConfigDto config : configs) {
            Symbol symbol = symbolRepo.findById(config.getSymbolId())
                    .orElseThrow(() -> new RuntimeException("Symbol not found: " + config.getSymbolId()));
            
            // Skip if already assigned
            if (groupSymbolRepo.findByGroupAndSymbol(group, symbol).isPresent()) {
                continue;
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
            
            savedSymbols.add(groupSymbolRepo.save(groupSymbol));
        }
        
        // Notify affected users
        List<Account> members = accountRepo.findByGroup(group);
        for (Account member : members) {
            Map<String, Object> userNotification = new HashMap<>();
            userNotification.put("type", "SYMBOL_ACCESS_CHANGED");
            userNotification.put("message", "New symbols added to your group");
            userNotification.put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSendToUser(
                member.getUsername(),
                "/queue/account",
                userNotification
            );
        }
        
        List<GroupSymbolDto> result = savedSymbols.stream()
                .map(GroupSymbolDto::new)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(result);
    } catch (RuntimeException e) {
        System.err.println("Error adding multiple symbols to group: " + e.getMessage());
        return ResponseEntity.badRequest().build();
    }
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

    @DeleteMapping("/{groupId}/members/{accountId}")
    public ResponseEntity<AccountDto> removeAccountFromGroup(@PathVariable Long groupId, 
                                                            @PathVariable Long accountId, 
                                                            HttpServletRequest request) {
        try {
                    adminValidationService.validateAdmin(request); 

            
            Account account = accountRepo.findById(accountId)
                    .orElseThrow(() -> new RuntimeException("Account not found"));
            
            if (account.getGroup() == null || !account.getGroup().getId().equals(groupId)) {
                throw new RuntimeException("Account is not in the specified group");
            }
            
            account.setGroup(null);
            Account saved = accountRepo.save(account);
            return ResponseEntity.ok(new AccountDto(saved));
        } catch (RuntimeException e) {
            System.err.println("Error removing account from group: " + e.getMessage());
            return ResponseEntity.status(403).build();
        }
    }

    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<?> removeUserFromGroup(@PathVariable Long groupId, @PathVariable Long userId, HttpServletRequest request) {
        try {
            adminValidationService.validateAdmin(request);            
            Account account = accountRepo.findById(userId).orElse(null);
            if (account == null) {
                return ResponseEntity.notFound().build();
            }
            
            account.setGroup(null);
            accountRepo.save(account);
            
            // Notify the user that their group has changed
            notificationService.notifyGroupChange(groupId, List.of(account.getUsername()), "You have been removed from your group");
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
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

/*    
    @PostMapping("/admin/broadcast-message")
public ResponseEntity<?> broadcastMessage(
        @RequestParam String message,
        @RequestParam(defaultValue = "info") String type,
        HttpServletRequest grouprequest) {
    
    if (!validateAdmin(grouprequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin access required");
        }
        
        Map<String, Object> broadcastMessage = new HashMap<>();
        broadcastMessage.put("type", "ADMIN_MESSAGE");
        broadcastMessage.put("message", message);
        broadcastMessage.put("messageType", type);
        broadcastMessage.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSend("/topic/admin-messages", broadcastMessage);
        
        return ResponseEntity.ok("Message broadcast successfully");
    }
    
        private boolean validateAdmin(HttpServletRequest grouprequest) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'validateAdmin'");
        }
    
        @PostMapping("/admin/emergency-stop-trading")
public ResponseEntity<?> emergencyStopTrading(
        @RequestParam String reason,
        HttpServletRequest grouprequest) {
    
    if (!validateAdmin(grouprequest)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Admin access required");
    }
    }
*/
}