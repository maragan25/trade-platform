package yes.example.no.controller;

import yes.example.no.dto.PriceUpdateDto;
import yes.example.no.entity.Account;
import yes.example.no.entity.Group;
import yes.example.no.entity.GroupSymbol;
import yes.example.no.entity.Symbol;
import yes.example.no.repository.AccountRepository;
import yes.example.no.repository.SymbolRepository;
import yes.example.no.service.AdminValidationService;
import yes.example.no.service.BalanceReconciliationService;

import yes.example.no.dto.PriceUpdateRequest;
import yes.example.no.dto.GroupSymbolConfig;
import yes.example.no.repository.GroupRepository;
import yes.example.no.repository.GroupSymbolRepository;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

import yes.example.no.service.GroupManagementService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.security.Principal;
import org.springframework.beans.factory.annotation.Autowired;


@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final BalanceReconciliationService reconciliationService;
    private final GroupManagementService groupManagementService;
    private final AdminValidationService adminValidationService; // Add this   

    @Autowired
    private SymbolRepository symbolRepo;
    
    @Autowired
    private AccountRepository accountRepo;  // ADD THIS
    
    @Autowired
    private GroupRepository groupRepo;      // ADD THIS
    
    @Autowired
    private GroupSymbolRepository groupSymbolRepo; // ADD THIS
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private PriceWebSocketController priceWebSocketController;
    

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

    @PostMapping("/price-update")
    public ResponseEntity<?> updatePrice(@RequestBody PriceUpdateRequest request, HttpServletRequest httpRequest) {
        try {
            adminValidationService.validateAdmin(httpRequest);
            
            Symbol symbol = symbolRepo.findById((long) request.getSymbolId()).orElse(null);
            if (symbol == null) {
                return ResponseEntity.badRequest().body("Symbol not found");
            }
            
            // Update symbol prices
            symbol.setBidPrice(request.getBidPrice());
            symbol.setAskPrice(request.getAskPrice());
            symbolRepo.save(symbol);
            
            // Create price update message
            Map<String, Object> priceUpdate = Map.of(
                "symbolId", symbol.getId(),
                "symbolName", symbol.getName(),
                "bidPrice", symbol.getBidPrice(),
                "askPrice", symbol.getAskPrice(),
                "timestamp", System.currentTimeMillis()
            );
            
            // Broadcast to all connected clients immediately
            messagingTemplate.convertAndSend("/topic/prices", priceUpdate);
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating price: " + e.getMessage());
        }
    }

    @PostMapping("/reconcile/{accountId}")
    public ResponseEntity<BalanceReconciliationService.ReconciliationResult> reconcileAccount(
            @PathVariable Long accountId, Principal principal, HttpServletRequest request) {
        try {
                    adminValidationService.validateAdmin(request);
            return ResponseEntity.ok(reconciliationService.reconcileAccount(accountId));
        } catch (RuntimeException e) {
            System.err.println("Error reconciling account: " + e.getMessage());
            return ResponseEntity.status(403).build();
        }
    }

    @PostMapping("/fix-balance/{accountId}")
    public ResponseEntity<Void> fixAccountBalance(@PathVariable Long accountId, 
                                                 @RequestParam String reason,
                                                 Principal principal,
                                                 HttpServletRequest request) {
        try {
                    adminValidationService.validateAdmin(request);
            
            Account admin = accountRepo.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("Admin account not found"));
            
            reconciliationService.fixAccountBalance(accountId, reason, admin.getUsername());
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            System.err.println("Error fixing balance: " + e.getMessage());
            return ResponseEntity.status(403).build();
        }
    }

    @GetMapping("/accounts")
    public ResponseEntity<List<AccountDto>> getAllAccounts(Principal principal, HttpServletRequest  request) {
        try {
                    adminValidationService.validateAdmin(request);
            List<Account> accounts = accountRepo.findAll();
            List<AccountDto> accountDtos = accounts.stream()
                    .map(AccountDto::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(accountDtos);
        } catch (RuntimeException e) {
            System.err.println("Error getting accounts: " + e.getMessage());
            return ResponseEntity.status(403).build();
        }
    }

    @GetMapping("/accounts/non-admin")
    public ResponseEntity<List<AccountDto>> getNonAdminAccounts(Principal principal, HttpServletRequest request) {
        try {
                    adminValidationService.validateAdmin(request);
            List<Account> nonAdminAccounts = accountRepo.findAll().stream()
                    .filter(account -> !account.isAdmin())
                    .collect(Collectors.toList());
            List<AccountDto> accountDtos = nonAdminAccounts.stream()
                    .map(AccountDto::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(accountDtos);
        } catch (RuntimeException e) {
            System.err.println("Error getting non-admin accounts: " + e.getMessage());
            return ResponseEntity.status(403).build();
        }
    }

    @GetMapping("/symbols")
    public ResponseEntity<List<SymbolDto>> getAllSymbols(Principal principal, HttpServletRequest request) {
        try {
                    adminValidationService.validateAdmin(request);
            List<Symbol> symbols = symbolRepo.findAll();
            List<SymbolDto> symbolDtos = symbols.stream()
                    .map(SymbolDto::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(symbolDtos);
        } catch (RuntimeException e) {
            System.err.println("Error getting symbols: " + e.getMessage());
            return ResponseEntity.status(403).build();
        }
    }

    @PostMapping("/symbols")
    public ResponseEntity<SymbolDto> createSymbol(@RequestBody Symbol symbol, Principal principal, HttpServletRequest request) {
        try {
                    adminValidationService.validateAdmin(request);
            
            if (symbolRepo.findByName(symbol.getName()).isPresent()) {
                throw new RuntimeException("Symbol with name '" + symbol.getName() + "' already exists");
            }
            
            if (symbol.getBidPrice() >= symbol.getAskPrice()) {
                throw new RuntimeException("Bid price must be lower than ask price");
            }
            
            symbol.setActive(true);
            Symbol saved = symbolRepo.save(symbol);
            return ResponseEntity.ok(new SymbolDto(saved));
        } catch (RuntimeException e) {
            System.err.println("Error creating symbol: " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/symbols/{symbolId}")
    public ResponseEntity<Void> deleteSymbol(@PathVariable Long symbolId, Principal principal, HttpServletRequest request) {
        try {
                    adminValidationService.validateAdmin(request);
            
            Symbol symbol = symbolRepo.findById(symbolId)
                    .orElseThrow(() -> new RuntimeException("Symbol not found"));
            
            // Deactivate instead of delete to maintain referential integrity
            symbol.setActive(false);
            symbolRepo.save(symbol);
            
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            System.err.println("Error deleting symbol: " + e.getMessage());
            return ResponseEntity.status(403).build();
        }
    }

    @PutMapping("/symbols/{symbolId}")
    public ResponseEntity<Symbol> updateSymbol(@PathVariable Long symbolId, 
                                              @RequestBody Symbol updatedSymbol, 
                                              Principal principal,
                                              HttpServletRequest    request) {
                adminValidationService.validateAdmin(request);
        
        Symbol symbol = symbolRepo.findById(symbolId)
                .orElseThrow(() -> new RuntimeException("Symbol not found"));
        
        // Check if name is being changed and if new name already exists
        if (!symbol.getName().equals(updatedSymbol.getName())) {
            if (symbolRepo.findByName(updatedSymbol.getName()).isPresent()) {
                throw new RuntimeException("Symbol with name '" + updatedSymbol.getName() + "' already exists");
            }
        }
        
        if (updatedSymbol.getBidPrice() >= updatedSymbol.getAskPrice()) {
            throw new RuntimeException("Bid price must be lower than ask price");
        }
        
        symbol.setName(updatedSymbol.getName());
        symbol.setBidPrice(updatedSymbol.getBidPrice());
        symbol.setAskPrice(updatedSymbol.getAskPrice());
        symbol.setActive(updatedSymbol.isActive());
        
        return ResponseEntity.ok(symbolRepo.save(symbol));
    }

    @PutMapping("/accounts/{accountId}/balance")
    public ResponseEntity<Account> adjustAccountBalance(@PathVariable Long accountId,
                                                       @RequestParam double amount,
                                                       @RequestParam String reason,
                                                       Principal principal,
                                                       HttpServletRequest   request) {
                adminValidationService.validateAdmin(request);
        
        Account account = accountRepo.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        if (account.isAdmin()) {
            throw new RuntimeException("Cannot adjust admin account balance");
        }
        
        double newBalance = account.getBalance() + amount;
        if (newBalance < 0) {
            throw new RuntimeException("Insufficient balance for adjustment");
        }
        
        account.setBalance(newBalance);
        account.setEquity(newBalance + (account.getEquity() - account.getBalance())); // Maintain unrealized P&L
        account.setFreeMargin(account.getEquity() - account.getMarginUsed());
        
        Account savedAccount = accountRepo.save(account);
        
        // Log the adjustment (you might want to create a separate audit log)
        System.out.println("Balance adjusted for account " + account.getUsername() + 
                          " by " + principal.getName() + 
                          ". Amount: " + amount + 
                          ", Reason: " + reason);
        
        return ResponseEntity.ok(savedAccount);
    }

    @PostMapping("/accounts/{accountId}/group/{groupId}")
    public ResponseEntity<?> assignAccountToGroup(@PathVariable Long accountId, @PathVariable Long   groupId) {
        try {
            Account account = accountRepo.findById(accountId).orElse(null);
            Group group = groupRepo.findById(groupId).orElse(null); // CHANGED from groupRepository
            
            if (account == null || group == null) {
                return ResponseEntity.badRequest().body("Account or Group not found");
            }
            
            account.setGroup(group);
            accountRepo.save(account); // CHANGED from accountRepository
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error assigning account to group");
        }
    }

    @GetMapping("/stats/overview")
    public ResponseEntity<Map<String, Object>> getAdminOverview(Principal principal, HttpServletRequest request) {
                adminValidationService.validateAdmin(request);
        
        List<Account> allAccounts = accountRepo.findAll();
        List<Account> nonAdminAccounts = allAccounts.stream()
                .filter(acc -> !acc.isAdmin())
                .toList();
        
        Map<String, Object> overview = Map.of(
            "totalUsers", nonAdminAccounts.size(),
            "totalBalance", nonAdminAccounts.stream().mapToDouble(Account::getBalance).sum(),
            "totalEquity", nonAdminAccounts.stream().mapToDouble(Account::getEquity).sum(),
            "totalSymbols", symbolRepo.count(),
            "activeSymbols", symbolRepo.findByActiveTrue().size(),
            "groupStats", groupManagementService.getGroupStatistics()
        );
        
        return ResponseEntity.ok(overview);
    }

    @PutMapping("/admin/groups/{groupId}")
    public ResponseEntity<?> updateGroup(@PathVariable Long groupId, @RequestBody Group groupData, HttpServletRequest request) {
        try {
            adminValidationService.validateAdmin(request);
            
            Group group = groupRepo.findById(groupId).orElse(null);
            if (group == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Get affected users before update
            List<String> affectedUsernames = group.getAccounts().stream()
                .map(Account::getUsername)
                .collect(Collectors.toList());
            
            // Update group
            group.setName(groupData.getName());
            group.setDescription(groupData.getDescription());
            group.setActive(groupData.isActive());
            groupRepo.save(group);
            
            // Broadcast group change
            Map<String, Object> groupUpdate = Map.of(
                "type", "GROUP_CHANGED",
                "groupId", groupId,
                "groupName", group.getName(),
                "affectedUsers", affectedUsernames,
                "message", "Group " + group.getName() + " has been updated"
            );
            
            messagingTemplate.convertAndSend("/topic/group-updates", groupUpdate);
            
            // Notify affected users to refresh their symbols
            priceWebSocketController.notifyGroupChange(groupId, affectedUsernames);
            
            return ResponseEntity.ok(group);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating group: " + e.getMessage());
        }
    }

    @PutMapping("/accounts/{accountId}/group/{groupId}")
    public ResponseEntity<?> assignUserToGroup(@PathVariable Long accountId, @PathVariable Long groupId, HttpServletRequest request) {
        try {
            adminValidationService.validateAdmin(request);
            
            Account account = accountRepo.findById(accountId).orElse(null);
            Group group = groupRepo.findById(groupId).orElse(null);
            
            if (account == null || group == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Update user's group
            account.setGroup(group);
            accountRepo.save(account);
            
            // Broadcast user assignment change
            Map<String, Object> assignmentUpdate = Map.of(
                "type", "GROUP_ASSIGNMENT_CHANGED",
                "userId", accountId,
                "username", account.getUsername(),
                "groupId", groupId,
                "groupName", group.getName(),
                "message", "Your group assignment has been updated"
            );
            
            // Send to specific user
            messagingTemplate.convertAndSendToUser(account.getUsername(), "/queue/notifications", assignmentUpdate);
            
            // Also broadcast generally
            messagingTemplate.convertAndSend("/topic/group-updates", assignmentUpdate);
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error assigning user: " + e.getMessage());
        }
    }
    
    @PostMapping("/groups/{groupId}/symbols/bulk")
    public ResponseEntity<?> addSymbolsToGroup(@PathVariable Long groupId, @RequestBody List<GroupSymbolConfig> configs) {
        try {
            Group group = groupRepo.findById(groupId).orElse(null); // CHANGED from groupRepo
            
            if (group == null) {
                return ResponseEntity.badRequest().body("Group not found");
            }
            
            for (GroupSymbolConfig config : configs) {
                Symbol symbol = symbolRepo.findById((long) config.getSymbolId()).orElse(null);
                if (symbol != null) {
                    // Check if mapping already exists
                    Optional<GroupSymbol> existing = groupSymbolRepo.findByGroupAndSymbol(group, symbol); // CHANGED repository name
                    
                    if (existing == null) {
                        GroupSymbol groupSymbol = new GroupSymbol();
                        groupSymbol.setGroup(group);
                        groupSymbol.setSymbol(symbol);
                        groupSymbol.setCanViewQuotes(config.isCanViewQuotes());
                        groupSymbol.setCanTrade(config.isCanTrade());
                        groupSymbol.setBidMarkup(config.getBidMarkup());
                        groupSymbol.setAskMarkup(config.getAskMarkup());
                        groupSymbol.setBidMarkupPercent(config.getBidMarkupPercent());
                        groupSymbol.setAskMarkupPercent(config.getAskMarkupPercent());
                        
                        groupSymbolRepo.save(groupSymbol); // CHANGED repository name
                    }
                }
            }
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error adding symbols to group");
        }
    }

    @PostMapping("/emergency-stop-trading")
    public ResponseEntity<?> emergencyStopTrading(@RequestParam String reason, HttpServletRequest request) {
        try {
            adminValidationService.validateAdmin(request);
            
            // Your emergency stop logic here...
            
            // Broadcast emergency message to all users
            Map<String, Object> emergencyMessage = Map.of(
                "type", "EMERGENCY_STOP",
                "message", "Trading has been suspended: " + reason,
                "timestamp", System.currentTimeMillis()
            );
            
            messagingTemplate.convertAndSend("/topic/admin-messages", emergencyMessage);
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/resume-trading")
    public ResponseEntity<?> resumeTrading(HttpServletRequest request) {
        try {
            adminValidationService.validateAdmin(request);
            
            // Your resume trading logic here...

            List<Symbol> symbols = symbolRepo.findByActiveTrue();
            symbols.forEach(symbol -> symbol.setActive(false));
            symbolRepo.saveAll(symbols);
            
            // Broadcast resume message
            Map<String, Object> resumeMessage = Map.of(
                "type", "TRADING_RESUMED",
                "message", "Trading has been resumed",
                "timestamp", System.currentTimeMillis()
            );
            
            messagingTemplate.convertAndSend("/topic/admin-messages", resumeMessage);
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/broadcast-message")
    public ResponseEntity<?> broadcastMessage(@RequestParam String message, @RequestParam String type, HttpServletRequest request) {
        try {
            adminValidationService.validateAdmin(request);
            
            Map<String, Object> broadcastMessage = Map.of(
                "type", "ADMIN_BROADCAST",
                "messageType", type,
                "message", message,
                "timestamp", System.currentTimeMillis()
            );
            
            messagingTemplate.convertAndSend("/topic/admin-messages", broadcastMessage);
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }
}

    /* 
    @PostMapping("/broadcast-message")
    public ResponseEntity<Void> broadcastMessage(@RequestParam String message, 
                                                @RequestParam(defaultValue = "info") String type,
                                                Principal principal) {
        validateAdmin(principal);
        
        Map<String, Object> broadcastData = Map.of(
            "type", "ADMIN_MESSAGE",
            "messageType", type,
            "message", message,
            "timestamp", System.currentTimeMillis(),
            "from", "Admin"
        );
        
        messagingTemplate.convertAndSend("/topic/admin-messages", broadcastData);
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/emergency-stop-trading")
    public ResponseEntity<Void> emergencyStopTrading(@RequestParam String reason, Principal principal) {
        validateAdmin(principal);
        
        // Deactivate all symbols to stop trading
        List<Symbol> symbols = symbolRepo.findByActiveTrue();
        symbols.forEach(symbol -> symbol.setActive(false));
        symbolRepo.saveAll(symbols);
        
        // Broadcast emergency message
        Map<String, Object> emergencyData = Map.of(
            "type", "EMERGENCY_STOP",
            "reason", reason,
            "timestamp", System.currentTimeMillis(),
            "from", principal.getName()
        );
        
        messagingTemplate.convertAndSend("/topic/admin-messages", emergencyData);
        
        System.out.println("EMERGENCY STOP: Trading halted by " + principal.getName() + 
                          ". Reason: " + reason);
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resume-trading")
    public ResponseEntity<Void> resumeTrading(Principal principal) {
        validateAdmin(principal);
        
        // Reactivate all symbols
        List<Symbol> symbols = symbolRepo.findAll();
        symbols.forEach(symbol -> symbol.setActive(true));
        symbolRepo.saveAll(symbols);
        
        // Broadcast resume message
        Map<String, Object> resumeData = Map.of(
            "type", "TRADING_RESUMED",
            "timestamp", System.currentTimeMillis(),
            "from", principal.getName()
        );
        
        messagingTemplate.convertAndSend("/topic/admin-messages", resumeData);
        
        System.out.println("Trading resumed by " + principal.getName());
        
        return ResponseEntity.ok().build();
    }
*/        