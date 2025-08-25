package yes.example.no.controller;

import yes.example.no.entity.*;
import yes.example.no.repository.*;
import yes.example.no.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/user/watchlist")
@RequiredArgsConstructor
public class UserWatchlistController {

    private final WatchlistService watchlistService;
    private final GroupService groupService;
    private final AccountRepository accountRepo;
    private final SymbolRepository symbolRepo;

    @GetMapping
    public List<Watchlist> getMyWatchlist(Principal principal) {
        Account account = accountRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return watchlistService.getWatchlistByAccount(account.getId());
    }

    @GetMapping("/available-symbols")
    public List<Symbol> getAvailableSymbolsForWatchlist(Principal principal) {
        Account account = accountRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        // Get symbols that the user's group can view (but user hasn't added to watchlist yet)
        List<Symbol> groupSymbols = groupService.getAvailableSymbolsForAccount(account);
        List<Watchlist> currentWatchlist = watchlistService.getWatchlistByAccount(account.getId());
        
        // Filter out symbols already in watchlist
        List<Long> watchlistSymbolIds = currentWatchlist.stream()
                .map(w -> w.getSymbol().getId())
                .toList();
        
        return groupSymbols.stream()
                .filter(symbol -> !watchlistSymbolIds.contains(symbol.getId()))
                .toList();
    }

    @PostMapping("/add/{symbolId}")
    public Watchlist addToMyWatchlist(@PathVariable Long symbolId, Principal principal) {
        Account account = accountRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        // Verify the user's group can view this symbol
        Symbol symbol = symbolRepo.findById(symbolId)
                .orElseThrow(() -> new RuntimeException("Symbol not found"));
        
        if (!groupService.canAccountViewSymbol(account, symbol)) {
            throw new RuntimeException("Not authorized to view this symbol");
        }
        
        return watchlistService.addToWatchlist(account.getId(), symbolId);
    }

    @DeleteMapping("/remove/{symbolId}")
    public void removeFromMyWatchlist(@PathVariable Long symbolId, Principal principal) {
        Account account = accountRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Account not found"));
        watchlistService.removeFromWatchlist(account.getId(), symbolId);
    }

    @PutMapping("/reorder")
    public void reorderMyWatchlist(@RequestBody List<Long> symbolIds, Principal principal) {
        Account account = accountRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Account not found"));
        watchlistService.reorderWatchlist(account.getId(), symbolIds);
    }

    // Admin endpoints for managing any user's watchlist
    @GetMapping("/{accountId}")
    public List<Watchlist> getWatchlist(@PathVariable Long accountId, Principal principal) {
        validateAdminOrOwner(principal, accountId);
        return watchlistService.getWatchlistByAccount(accountId);
    }

    @PostMapping("/{accountId}/add/{symbolId}")
    public Watchlist addToWatchlist(@PathVariable Long accountId, @PathVariable Long symbolId, Principal principal) {
        validateAdminOrOwner(principal, accountId);
        return watchlistService.addToWatchlist(accountId, symbolId);
    }

    @DeleteMapping("/{accountId}/remove/{symbolId}")
    public void removeFromWatchlist(@PathVariable Long accountId, @PathVariable Long symbolId, Principal principal) {
        validateAdminOrOwner(principal, accountId);
        watchlistService.removeFromWatchlist(accountId, symbolId);
    }

    @PutMapping("/{accountId}/reorder")
    public void reorderWatchlist(@PathVariable Long accountId, @RequestBody List<Long> symbolIds, Principal principal) {
        validateAdminOrOwner(principal, accountId);
        watchlistService.reorderWatchlist(accountId, symbolIds);
    }

    private void validateAdminOrOwner(Principal principal, Long accountId) {
        Account currentUser = accountRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        if (!currentUser.isAdmin() && !currentUser.getId().equals(accountId)) {
            throw new RuntimeException("Access denied");
        }
    }
}