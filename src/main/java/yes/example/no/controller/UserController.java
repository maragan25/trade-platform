package yes.example.no.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import yes.example.no.entity.Account;
import yes.example.no.entity.GroupSymbol;
import yes.example.no.entity.Symbol;
import yes.example.no.repository.AccountRepository;
import yes.example.no.repository.GroupSymbolRepository;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private AccountRepository accountRepo;
    
    @Autowired
    private GroupSymbolRepository groupSymbolRepo;

    // Get user's watchlist (symbols they have access to)
    @GetMapping("/watchlist")
    public ResponseEntity<?> getUserWatchlist(HttpSession session) {
        try {
            String username = (String) session.getAttribute("username");
            if (username == null) {
                return ResponseEntity.status(401).body("Not authenticated");
            }

            Account account = accountRepo.findByUsername(username).orElse(null);
            if (account == null) {
                return ResponseEntity.status(404).body("Account not found");
            }

            List<Map<String, Object>> watchlist = new ArrayList<>();
            
            if (account.getGroup() != null) {
                List<GroupSymbol> groupSymbols = groupSymbolRepo.findByGroup(account.getGroup());
                
                watchlist = groupSymbols.stream()
                    .filter(gs -> gs.isCanViewQuotes()) // Only include symbols user can view
                    .map(gs -> {
                        Map<String, Object> item = new HashMap<>();
                        Symbol symbol = gs.getSymbol();
                        
                        // Apply markups to prices
                        double bidPrice = symbol.getBidPrice() - gs.getBidMarkup() - (symbol.getBidPrice() * gs.getBidMarkupPercent() / 100);
                        double askPrice = symbol.getAskPrice() + gs.getAskMarkup() + (symbol.getAskPrice() * gs.getAskMarkupPercent() / 100);
                        
                        Map<String, Object> symbolData = new HashMap<>();
                        symbolData.put("id", symbol.getId());
                        symbolData.put("name", symbol.getName());
                        symbolData.put("bidPrice", bidPrice);
                        symbolData.put("askPrice", askPrice);
                        symbolData.put("active", symbol.isActive());
                        
                        item.put("symbol", symbolData);
                        item.put("groupSymbol", gs);
                        
                        return item;
                    })
                    .collect(Collectors.toList());
            }

            return ResponseEntity.ok(watchlist);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error loading watchlist: " + e.getMessage());
        }
    }


    // Get user's tradeable symbols (symbols they can trade)
    @GetMapping("/tradeable-symbols")
    public ResponseEntity<?> getTradeableSymbols(HttpSession session) {
        try {
            String username = (String) session.getAttribute("username");
            if (username == null) {
                return ResponseEntity.status(401).body("Not authenticated");
            }

            Account account = accountRepo.findByUsername(username).orElse(null);
            if (account == null) {
                return ResponseEntity.status(404).body("Account not found");
            }

            List<Map<String, Object>> tradeableSymbols = new ArrayList<>();
            
            if (account.getGroup() != null) {
                List<GroupSymbol> groupSymbols = groupSymbolRepo.findByGroup(account.getGroup());
                
                tradeableSymbols = groupSymbols.stream()
                    .filter(gs -> gs.isCanTrade()) // Only include symbols user can trade
                    .map(gs -> {
                        Map<String, Object> item = new HashMap<>();
                        Symbol symbol = gs.getSymbol();
                        
                        // Apply markups to prices
                        double bidPrice = symbol.getBidPrice() - gs.getBidMarkup() - (symbol.getBidPrice() * gs.getBidMarkupPercent() / 100);
                        double askPrice = symbol.getAskPrice() + gs.getAskMarkup() + (symbol.getAskPrice() * gs.getAskMarkupPercent() / 100);
                        
                        Map<String, Object> symbolData = new HashMap<>();
                        symbolData.put("id", symbol.getId());
                        symbolData.put("name", symbol.getName());
                        symbolData.put("bidPrice", bidPrice);
                        symbolData.put("askPrice", askPrice);
                        symbolData.put("active", symbol.isActive());
                        
                        item.put("symbol", symbolData);
                        item.put("canTrade", gs.isCanTrade());
                        item.put("canViewQuotes", gs.isCanViewQuotes());
                        
                        return item;
                    })
                    .collect(Collectors.toList());
            }

            return ResponseEntity.ok(tradeableSymbols);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error loading tradeable symbols: " + e.getMessage());
        }
    }

    // Get user profile information
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(HttpSession session) {
        try {
            String username = (String) session.getAttribute("username");
            if (username == null) {
                return ResponseEntity.status(401).body("Not authenticated");
            }

            Account account = accountRepo.findByUsername(username).orElse(null);
            if (account == null) {
                return ResponseEntity.status(404).body("Account not found");
            }

            Map<String, Object> profile = new HashMap<>();
            profile.put("id", account.getId());
            profile.put("username", account.getUsername());
            profile.put("balance", account.getBalance());
            profile.put("equity", account.getEquity());
            profile.put("marginUsed", account.getMarginUsed());
            profile.put("freeMargin", account.getFreeMargin());
            profile.put("admin", account.isAdmin());
            
            if (account.getGroup() != null) {
                Map<String, Object> groupInfo = new HashMap<>();
                groupInfo.put("id", account.getGroup().getId());
                groupInfo.put("name", account.getGroup().getName());
                groupInfo.put("description", account.getGroup().getDescription());
                groupInfo.put("active", account.getGroup().isActive());
                profile.put("group", groupInfo);
            }

            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error loading user profile: " + e.getMessage());
        }
    }

    // Get user's current prices with markups applied
    @GetMapping("/prices")
    public ResponseEntity<?> getUserPrices(HttpSession session) {
        try {
            String username = (String) session.getAttribute("username");
            if (username == null) {
                return ResponseEntity.status(401).body("Not authenticated");
            }

            Account account = accountRepo.findByUsername(username).orElse(null);
            if (account == null) {
                return ResponseEntity.status(404).body("Account not found");
            }

            List<Map<String, Object>> prices = new ArrayList<>();
            
            if (account.getGroup() != null) {
                List<GroupSymbol> groupSymbols = groupSymbolRepo.findByGroup(account.getGroup());
                
                prices = groupSymbols.stream()
                    .filter(gs -> gs.isCanViewQuotes())
                    .map(gs -> {
                        Symbol symbol = gs.getSymbol();
                        
                        // Apply markups to prices
                        double bidPrice = symbol.getBidPrice() - gs.getBidMarkup() - (symbol.getBidPrice() * gs.getBidMarkupPercent() / 100);
                        double askPrice = symbol.getAskPrice() + gs.getAskMarkup() + (symbol.getAskPrice() * gs.getAskMarkupPercent() / 100);
                        
                        Map<String, Object> priceData = new HashMap<>();
                        priceData.put("symbolId", symbol.getId());
                        priceData.put("symbolName", symbol.getName());
                        priceData.put("bidPrice", bidPrice);
                        priceData.put("askPrice", askPrice);
                        priceData.put("timestamp", System.currentTimeMillis());
                        
                        return priceData;
                    })
                    .collect(Collectors.toList());
            }

            return ResponseEntity.ok(prices);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error loading prices: " + e.getMessage());
        }
    }

    // Check if user can trade a specific symbol
    @GetMapping("/can-trade/{symbolId}")
    public ResponseEntity<?> canTradeSymbol(@PathVariable int symbolId, HttpSession session) {
        try {
            String username = (String) session.getAttribute("username");
            if (username == null) {
                return ResponseEntity.status(401).body("Not authenticated");
            }

            Account account = accountRepo.findByUsername(username).orElse(null);
            if (account == null) {
                return ResponseEntity.status(404).body("Account not found");
            }

            boolean canTrade = false;
            
            if (account.getGroup() != null) {
                List<GroupSymbol> groupSymbols = groupSymbolRepo.findByGroup(account.getGroup());
                canTrade = groupSymbols.stream()
                    .anyMatch(gs -> gs.getSymbol().getId() == symbolId && gs.isCanTrade());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("canTrade", canTrade);
            response.put("symbolId", symbolId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error checking trade permission: " + e.getMessage());
        }
    }

    // Get symbol with markup applied for specific user
    @GetMapping("/symbol/{symbolId}")
    public ResponseEntity<?> getUserSymbol(@PathVariable int symbolId, HttpSession session) {
        try {
            String username = (String) session.getAttribute("username");
            if (username == null) {
                return ResponseEntity.status(401).body("Not authenticated");
            }

            Account account = accountRepo.findByUsername(username).orElse(null);
            if (account == null) {
                return ResponseEntity.status(404).body("Account not found");
            }

            if (account.getGroup() == null) {
                return ResponseEntity.status(403).body("No group assigned");
            }

            List<GroupSymbol> groupSymbols = groupSymbolRepo.findByGroup(account.getGroup());
            GroupSymbol groupSymbol = groupSymbols.stream()
                .filter(gs -> gs.getSymbol().getId() == symbolId)
                .findFirst()
                .orElse(null);

            if (groupSymbol == null || !groupSymbol.isCanViewQuotes()) {
                return ResponseEntity.status(403).body("Symbol not accessible");
            }

            Symbol symbol = groupSymbol.getSymbol();
            
            // Apply markups to prices
            double bidPrice = symbol.getBidPrice() - groupSymbol.getBidMarkup() - (symbol.getBidPrice() * groupSymbol.getBidMarkupPercent() / 100);
            double askPrice = symbol.getAskPrice() + groupSymbol.getAskMarkup() + (symbol.getAskPrice() * groupSymbol.getAskMarkupPercent() / 100);
            
            Map<String, Object> response = new HashMap<>();
            response.put("symbolId", symbol.getId());
            response.put("symbolName", symbol.getName());
            response.put("bidPrice", bidPrice);
            response.put("askPrice", askPrice);
            response.put("canTrade", groupSymbol.isCanTrade());
            response.put("canViewQuotes", groupSymbol.isCanViewQuotes());
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error loading symbol: " + e.getMessage());
        }
    }
}