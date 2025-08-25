package yes.example.no.service;

import yes.example.no.entity.*;
import yes.example.no.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupService {
    
    private final GroupRepository groupRepo;
    private final GroupSymbolRepository groupSymbolRepo;
    private final SymbolRepository symbolRepo;

    public boolean canAccountViewSymbol(Account account, Symbol symbol) {
        if (account.isAdmin()) return true;
        if (account.getGroup() == null) return false;
        
        return groupSymbolRepo.findByGroupAndSymbol(account.getGroup(), symbol)
                .map(GroupSymbol::isCanViewQuotes)
                .orElse(false);
    }

    public boolean canAccountTradeSymbol(Account account, Symbol symbol) {
        if (account.isAdmin()) return true;
        if (account.getGroup() == null) return false;
        
        return groupSymbolRepo.findByGroupAndSymbol(account.getGroup(), symbol)
                .map(GroupSymbol::isCanTrade)
                .orElse(false);
    }

    public double getAdjustedBidPrice(Account account, Symbol symbol) {
        if (account.getGroup() == null) return symbol.getBidPrice();
        
        GroupSymbol groupSymbol = groupSymbolRepo.findByGroupAndSymbol(account.getGroup(), symbol)
                .orElse(null);
        
        if (groupSymbol == null) return symbol.getBidPrice();
        
        double price = symbol.getBidPrice();
        price += groupSymbol.getBidMarkup();
        price += price * (groupSymbol.getBidMarkupPercent() / 100.0);
        
        return price;
    }

    public double getAdjustedAskPrice(Account account, Symbol symbol) {
        if (account.getGroup() == null) return symbol.getAskPrice();
        
        GroupSymbol groupSymbol = groupSymbolRepo.findByGroupAndSymbol(account.getGroup(), symbol)
                .orElse(null);
        
        if (groupSymbol == null) return symbol.getAskPrice();
        
        double price = symbol.getAskPrice();
        price += groupSymbol.getAskMarkup();
        price += price * (groupSymbol.getAskMarkupPercent() / 100.0);
        
        return price;
    }

    public List<Symbol> getAvailableSymbolsForAccount(Account account) {
        if (account.isAdmin()) {
            return symbolRepo.findByActiveTrue();
        }
        
        if (account.getGroup() == null) {
            return List.of();
        }
        
        return groupSymbolRepo.findSymbolsByGroupAndCanViewQuotesTrue(account.getGroup());
    }
}    
