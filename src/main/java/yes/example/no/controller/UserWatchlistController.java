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

    // Admin endpoints for managing any user's watchlist
    @GetMapping("/{accountId}")
    public List<Watchlist> getWatchlist(@PathVariable Long accountId, Principal principal) {
        validateAdminOrOwner(principal, accountId);
        return watchlistService.getWatchlistByAccount(accountId);
    }

    private void validateAdminOrOwner(Principal principal, Long accountId) {
        Account currentUser = accountRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("Account not found"));
        
        if (!currentUser.isAdmin() && !currentUser.getId().equals(accountId)) {
            throw new RuntimeException("Access denied");
        }
    }
}