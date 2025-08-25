package yes.example.no.service;

import yes.example.no.entity.*;
import yes.example.no.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class WatchlistService {
    
    private final WatchlistRepository watchlistRepo;
    private final AccountRepository accountRepo;
    private final SymbolRepository symbolRepo;

    public List<Watchlist> getWatchlistByAccount(Long accountId) {
        Account account = accountRepo.findById(accountId).orElseThrow();
        return watchlistRepo.findByAccountOrderBySortOrder(account);
    }

    public Watchlist addToWatchlist(Long accountId, Long symbolId) {
        Account account = accountRepo.findById(accountId).orElseThrow();
        Symbol symbol = symbolRepo.findById(symbolId).orElseThrow();
        
        // Check if already exists
        Optional<Watchlist> existing = watchlistRepo.findByAccountAndSymbol(account, symbol);
        if (existing.isPresent()) {
            throw new RuntimeException("Symbol already in watchlist");
        }

        Watchlist watchlist = new Watchlist();
        watchlist.setAccount(account);
        watchlist.setSymbol(symbol);
        watchlist.setSortOrder(getNextSortOrder(account));
        
        return watchlistRepo.save(watchlist);
    }

    public void removeFromWatchlist(Long accountId, Long symbolId) {
        Account account = accountRepo.findById(accountId).orElseThrow();
        Symbol symbol = symbolRepo.findById(symbolId).orElseThrow();
        
        Watchlist watchlist = watchlistRepo.findByAccountAndSymbol(account, symbol)
                .orElseThrow(() -> new RuntimeException("Symbol not in watchlist"));
        
        watchlistRepo.delete(watchlist);
    }

    public void reorderWatchlist(Long accountId, List<Long> symbolIds) {
        Account account = accountRepo.findById(accountId).orElseThrow();
        
        for (int i = 0; i < symbolIds.size(); i++) {
            Symbol symbol = symbolRepo.findById(symbolIds.get(i)).orElseThrow();
            Watchlist watchlist = watchlistRepo.findByAccountAndSymbol(account, symbol)
                    .orElseThrow();
            watchlist.setSortOrder(i);
            watchlistRepo.save(watchlist);
        }
    }

    private int getNextSortOrder(Account account) {
        return watchlistRepo.findMaxSortOrderByAccount(account).orElse(-1) + 1;
    }
}
