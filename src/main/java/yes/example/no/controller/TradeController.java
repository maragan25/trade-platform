package yes.example.no.controller;

import yes.example.no.dto.TradeRequest;
import yes.example.no.entity.Account;
import yes.example.no.entity.Trade;
import yes.example.no.repository.AccountRepository;
import yes.example.no.service.TradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

    private final AccountRepository accountRepo;
    private final TradeService tradeService;

    @PostMapping("/open")
    public Trade openTrade(@RequestBody TradeRequest request, Principal principal) {
        // Get the current user's account
        Account account = accountRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        // Set the account ID in the request
        request.setAccountId(account.getId());
        
        return tradeService.openTrade(request);
    }

    @PostMapping("/close")
    public Trade closeTrade(@RequestParam Long tradeId, @RequestParam double closingPrice, Principal principal) {
        // Verify the trade belongs to the current user
        Account account = accountRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        return tradeService.closeTrade(tradeId, closingPrice, account);
    }

    @GetMapping("/open")
    public List<Trade> getOpenTrades(Principal principal) {
        Account account = accountRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        return tradeService.getOpenTrades(account.getId());
    }

    @GetMapping("/history")
    public List<Trade> getTradeHistory(Principal principal) {
        Account account = accountRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        return tradeService.getTradeHistory(account.getId());
    }
}