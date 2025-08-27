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
    
    private final WatchlistRepository watchlistRepository;
    private final AccountRepository accountRepository;
    private final SymbolRepository symbolRepository;

    public List<Watchlist> getWatchlistByAccount(Long accountId) {
        return watchlistRepository.findByAccountIdOrderByOrderIndexAsc(accountId);
    }

    @Transactional
    public Watchlist addToWatchlist(Long accountId, Long symbolId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        Symbol symbol = symbolRepository.findById(symbolId)
                .orElseThrow(() -> new RuntimeException("Symbol not found"));

        // Check if already in watchlist
        Optional<Watchlist> existing = watchlistRepository.findByAccountAndSymbol(account, symbol);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Get next order index
        Integer maxOrder = watchlistRepository.findMaxOrderIndexByAccount(account);
        int nextOrder = (maxOrder != null) ? maxOrder + 1 : 0;

        Watchlist watchlistItem = new Watchlist(account, symbol);
        watchlistItem.setOrderIndex(nextOrder);
        
        return watchlistRepository.save(watchlistItem);
    }

    @Transactional
    public void removeFromWatchlist(Long accountId, Long symbolId) {
        watchlistRepository.deleteByAccountIdAndSymbolId(accountId, symbolId);
    }

    @Transactional
    public void reorderWatchlist(Long accountId, List<Long> symbolIds) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        for (int i = 0; i < symbolIds.size(); i++) {
            Long symbolId = symbolIds.get(i);
            Symbol symbol = symbolRepository.findById(symbolId)
                    .orElseThrow(() -> new RuntimeException("Symbol not found"));
            
            Optional<Watchlist> watchlistItem = watchlistRepository.findByAccountAndSymbol(account, symbol);
            if (watchlistItem.isPresent()) {
                watchlistItem.get().setOrderIndex(i);
                watchlistRepository.save(watchlistItem.get());
            }
        }
    }

    @Transactional
    public void addMultipleToWatchlist(Long accountId, List<Long> symbolIds) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        Integer maxOrder = watchlistRepository.findMaxOrderIndexByAccount(account);
        int currentOrder = (maxOrder != null) ? maxOrder + 1 : 0;

        for (Long symbolId : symbolIds) {
            Symbol symbol = symbolRepository.findById(symbolId)
                    .orElseThrow(() -> new RuntimeException("Symbol not found: " + symbolId));

            // Check if already in watchlist
            Optional<Watchlist> existing = watchlistRepository.findByAccountAndSymbol(account, symbol);
            if (existing.isEmpty()) {
                Watchlist watchlistItem = new Watchlist(account, symbol);
                watchlistItem.setOrderIndex(currentOrder++);
                watchlistRepository.save(watchlistItem);
            }
        }
    }
    
    private int getNextOrderIndex(Account account) {
        Integer maxOrder = watchlistRepository.findMaxOrderIndexByAccount(account);
        return (maxOrder != null ? maxOrder : -1) + 1;
    }
    
}
