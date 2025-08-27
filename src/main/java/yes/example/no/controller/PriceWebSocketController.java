package yes.example.no.controller;

import yes.example.no.dto.*;
import yes.example.no.entity.Account;
import yes.example.no.entity.Symbol;
import yes.example.no.repository.AccountRepository;
import yes.example.no.repository.SymbolRepository;
import yes.example.no.service.GroupService;
import yes.example.no.service.PriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class PriceWebSocketController {

    int check;

    private final SimpMessagingTemplate messagingTemplate;
    private final AccountRepository accountRepo;
    private final SymbolRepository symbolRepo;
    private final GroupService groupService;
    private final PriceService priceService;

    public void notifyGroupChange(Long groupId, List<String> affectedUsernames) {
        Map<String, Object> notification = Map.of(
            "type", "GROUP_SYMBOLS_CHANGED",
            "groupId", groupId,
            "affectedUsers", affectedUsernames,
            "message", "Your available symbols have been updated"
        );
        
        // Send to affected users
        affectedUsernames.forEach(username -> {
            messagingTemplate.convertAndSendToUser(username, "/queue/notifications", notification);
        });
    }

    @MessageMapping("/subscribe")
    @SendToUser("/queue/prices")
    public List<SymbolPriceDto> subscribeToSymbols(Principal principal, @Payload(required = false) SubscribeRequest request) {
        String username = null;
        
        // Try to get username from principal first
        if (principal != null) {
            username = principal.getName();
        }
        // If principal is null, try to get from request payload
        else if (request != null && request.getUsername() != null) {
            username = request.getUsername();
        }
        
        if (username == null) {
            // Return empty list or default symbols for unauthenticated users
            return symbolRepo.findByActiveTrue().stream()
                    .map(symbol -> new SymbolPriceDto(
                        symbol.getId(),
                        symbol.getName(),
                        symbol.getBidPrice(),
                        symbol.getAskPrice(),
                        System.currentTimeMillis()
                    ))
                    .collect(Collectors.toList());
        }

        Account account = accountRepo.findByUsername(username).orElse(null);
        if (account == null) {
            return List.of();
        }

        List<Symbol> availableSymbols = groupService.getAvailableSymbolsForAccount(account);
        
        return availableSymbols.stream()
                .map(symbol -> new SymbolPriceDto(
                    symbol.getId(),
                    symbol.getName(),
                    groupService.getAdjustedBidPrice(account, symbol),
                    groupService.getAdjustedAskPrice(account, symbol),
                    System.currentTimeMillis()
                ))
                .collect(Collectors.toList());
    }

    @MessageMapping("/admin/price-update")
    @SendTo("/topic/prices")
    public PriceUpdateDto updatePrice(@Payload PriceUpdateDto priceUpdate, Principal principal) {
        String username = principal != null ? principal.getName() : null;
        
        if (username == null) {
            throw new RuntimeException("Authentication required");
        }
        
        Account account = accountRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.isAdmin()) {
            throw new RuntimeException("Unauthorized: Admin access required");
        }

        Symbol symbol = symbolRepo.findById(priceUpdate.getSymbolId())
                .orElseThrow(() -> new RuntimeException("Symbol not found"));

        symbol.setBidPrice(priceUpdate.getBidPrice());
        symbol.setAskPrice(priceUpdate.getAskPrice());
        symbolRepo.save(symbol);

        // Store price history
        priceService.savePriceHistory(symbol, priceUpdate.getBidPrice(), priceUpdate.getAskPrice());

        // Broadcast to all users with personalized prices
        broadcastPriceUpdate(symbol);

        return priceUpdate;
    }

    @MessageMapping("/account-update")
    @SendToUser("/queue/account")
    public AccountUpdateDto getAccountUpdate(Principal principal, @Payload(required = false) SubscribeRequest request) {
        String username = null;
        
        if (principal != null) {
            username = principal.getName();
        } else if (request != null && request.getUsername() != null) {
            username = request.getUsername();
        }
        
        if (username == null) {
            throw new RuntimeException("Authentication required");
        }
        
        Account account = accountRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        return new AccountUpdateDto(
            account.getId(),
            account.getBalance(),
            account.getEquity(),
            account.getMarginUsed(),
            account.getFreeMargin(),
            System.currentTimeMillis()
        );
    }

    void broadcastPriceUpdate(Symbol symbol) {
        // Get all accounts that can view this symbol
        List<Account> allAccounts = accountRepo.findAll();
        
        for (Account account : allAccounts) {
            if (groupService.canAccountViewSymbol(account, symbol)) {
                SymbolPriceDto personalizedPrice = new SymbolPriceDto(
                    symbol.getId(),
                    symbol.getName(),
                    groupService.getAdjustedBidPrice(account, symbol),
                    groupService.getAdjustedAskPrice(account, symbol),
                    System.currentTimeMillis()
                );
                
                messagingTemplate.convertAndSendToUser(
                    account.getUsername(),
                    "/queue/prices",
                    personalizedPrice
                );
            }
        }
    }
    
    @MessageMapping("/admin/broadcast")
    public void broadcastAdminMessage(@Payload Map<String, Object> message, Principal principal) {
        // Verify admin privileges
        if (principal != null) {
            Account account = accountRepo.findByUsername(principal.getName()).orElse(null);
            if (account != null && account.isAdmin()) {
                messagingTemplate.convertAndSend("/topic/admin-messages", message);
                System.out.println("Admin message broadcasted: " + message);
            }
        }
    }

    @MessageMapping("/price/broadcast") 
    public void broadcastPriceUpdate(@Payload Map<String, Object> priceUpdate, Principal principal) {
        // Verify admin privileges
        if (principal != null) {
            Account account = accountRepo.findByUsername(principal.getName()).orElse(null);
            if (account != null && account.isAdmin()) {
                messagingTemplate.convertAndSend("/topic/prices", priceUpdate);
                System.out.println("Price update broadcasted: " + priceUpdate);
            }
        }
    }

    @MessageMapping("/group/broadcast")
    public void broadcastGroupChange(@Payload Map<String, Object> groupMessage, Principal principal) {
        // Verify admin privileges
        if (principal != null) {
            Account account = accountRepo.findByUsername(principal.getName()).orElse(null);
            if (account != null && account.isAdmin()) {
                messagingTemplate.convertAndSend("/topic/group-updates", groupMessage);
                System.out.println("Group update broadcasted: " + groupMessage);
            }
        }
    }

    public void broadcastToUsers(java.util.List<String> usernames, Object message, String destination) {
        for (String username : usernames) {
            messagingTemplate.convertAndSendToUser(username, destination, message);
        }
    }

    // DTO for subscribe requests
    public static class SubscribeRequest {
        private String username;
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }
}