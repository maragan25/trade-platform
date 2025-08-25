
package yes.example.no.service;

import yes.example.no.dto.AccountUpdateDto;
import yes.example.no.dto.TradeRequest;
import yes.example.no.entity.Account;
import yes.example.no.entity.Symbol;
import yes.example.no.entity.Trade;
import yes.example.no.repository.AccountRepository;
import yes.example.no.repository.SymbolRepository;
import yes.example.no.repository.TradeRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TradeService {

    private final AccountRepository accountRepo;
    private final SymbolRepository symbolRepo;
    private final TradeRepository tradeRepo;
    private final GroupService groupService;
    private final SimpMessagingTemplate messagingTemplate;

    public Trade openTrade(TradeRequest request) {
        Account account = accountRepo.findById(request.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found"));
        Symbol symbol = symbolRepo.findById(request.getSymbolId())
                .orElseThrow(() -> new RuntimeException("Symbol not found"));

        // Check if account can trade this symbol (skip for admin or if no group system)
        if (account.getGroup() != null && !groupService.canAccountTradeSymbol(account, symbol)) {
            throw new RuntimeException("Account not authorized to trade this symbol");
        }

        double size = request.getSize();
        double openingPrice = request.getOpeningPrice();
        String type = request.getType();

        // Validate opening price against current market prices
        validateOpeningPrice(account, symbol, type, openingPrice);

        double marginRequired = calculateMargin(size, openingPrice);
        if (account.getFreeMargin() < marginRequired) {
            throw new RuntimeException("Insufficient free margin. Required: " + marginRequired + 
                                     ", Available: " + account.getFreeMargin());
        }

        // Update account margin
        account.setMarginUsed(account.getMarginUsed() + marginRequired);
        updateEquity(account);
        accountRepo.save(account);

        // Create trade
        Trade trade = new Trade();
        trade.setAccount(account);
        trade.setSymbol(symbol);
        trade.setType(type.toUpperCase());
        trade.setSize(size);
        trade.setOpeningPrice(openingPrice);
        trade.setOpen(true);
        trade.setUniqueTradeNo(generateUniqueTradeNo());
        trade.setMargin(marginRequired);
        trade.setOpenTime(LocalDateTime.now());

        Trade savedTrade = tradeRepo.save(trade);

        // Send real-time update to client
        sendAccountUpdate(account);

        return savedTrade;
    }

    public Trade closeTrade(Long tradeId, double closingPrice, Account requestingAccount) {
        Trade trade = tradeRepo.findById(tradeId)
                .orElseThrow(() -> new RuntimeException("Trade not found"));

        // Verify trade belongs to requesting account (security check)
        if (!trade.getAccount().getId().equals(requestingAccount.getId()) && !requestingAccount.isAdmin()) {
            throw new RuntimeException("Unauthorized: Trade does not belong to this account");
        }

        if (!trade.isOpen()) {
            throw new RuntimeException("Trade already closed");
        }

        // Validate closing price
        validateClosingPrice(trade.getAccount(), trade.getSymbol(), trade.getType(), closingPrice);

        trade.setClosingPrice(closingPrice);
        trade.setOpen(false);
        trade.setCloseTime(LocalDateTime.now());

        // Calculate profit or loss
        double profit = calculateProfit(trade, closingPrice);
        trade.setRealizedProfit(profit);

        Account account = trade.getAccount();

        // Update account margin and balance
        account.setMarginUsed(account.getMarginUsed() - trade.getMargin());
        account.setBalance(account.getBalance() + profit);

        // Update equity and free margin
        updateEquity(account);

        // Save changes
        accountRepo.save(account);
        Trade savedTrade = tradeRepo.save(trade);

        // Send real-time update to client
        sendAccountUpdate(account);

        return savedTrade;
    }

    public List<Trade> getOpenTrades(Long accountId) {
        Account account = accountRepo.findById(accountId).orElseThrow();
        return tradeRepo.findByAccountAndOpenTrue(account);
    }

    public List<Trade> getTradeHistory(Long accountId) {
        Account account = accountRepo.findById(accountId).orElseThrow();
        return tradeRepo.findByAccountAndOpenFalseOrderByCloseTime(account);
    }

    public double calculateUnrealizedProfit(Account account) {
        List<Trade> openTrades = tradeRepo.findByAccountAndOpenTrue(account);
        double totalUnrealizedProfit = 0.0;

        for (Trade trade : openTrades) {
            Symbol symbol = trade.getSymbol();
            double currentPrice;
            
            if ("BUY".equalsIgnoreCase(trade.getType())) {
                currentPrice = getAdjustedPrice(account, symbol, false); // bid for closing buy
            } else {
                currentPrice = getAdjustedPrice(account, symbol, true); // ask for closing sell
            }
            
            totalUnrealizedProfit += trade.calculateUnrealizedProfit(currentPrice);
        }

        return totalUnrealizedProfit;
    }

    private double getAdjustedPrice(Account account, Symbol symbol, boolean isAsk) {
        // Use group service if available, otherwise use raw symbol prices
        if (account.getGroup() != null) {
            return isAsk ? groupService.getAdjustedAskPrice(account, symbol) 
                        : groupService.getAdjustedBidPrice(account, symbol);
        } else {
            return isAsk ? symbol.getAskPrice() : symbol.getBidPrice();
        }
    }

    private void validateOpeningPrice(Account account, Symbol symbol, String type, double openingPrice) {
        double bidPrice = getAdjustedPrice(account, symbol, false);
        double askPrice = getAdjustedPrice(account, symbol, true);
        
        if ("BUY".equalsIgnoreCase(type)) {
            // For buy orders, price should be close to ask price
            if (Math.abs(openingPrice - askPrice) > askPrice * 0.01) { // 1% tolerance
                throw new RuntimeException("Opening price too far from market price. Expected: " + askPrice);
            }
        } else if ("SELL".equalsIgnoreCase(type)) {
            // For sell orders, price should be close to bid price
            if (Math.abs(openingPrice - bidPrice) > bidPrice * 0.01) { // 1% tolerance
                throw new RuntimeException("Opening price too far from market price. Expected: " + bidPrice);
            }
        }
    }

    private void validateClosingPrice(Account account, Symbol symbol, String type, double closingPrice) {
        double bidPrice = getAdjustedPrice(account, symbol, false);
        double askPrice = getAdjustedPrice(account, symbol, true);
        
        if ("BUY".equalsIgnoreCase(type)) {
            // For closing buy positions, use bid price
            if (Math.abs(closingPrice - bidPrice) > bidPrice * 0.01) {
                throw new RuntimeException("Closing price too far from market price. Expected: " + bidPrice);
            }
        } else if ("SELL".equalsIgnoreCase(type)) {
            // For closing sell positions, use ask price
            if (Math.abs(closingPrice - askPrice) > askPrice * 0.01) {
                throw new RuntimeException("Closing price too far from market price. Expected: " + askPrice);
            }
        }
    }

    private double calculateMargin(double size, double price) {
        double leverage = 100; // 1:100 leverage
        return (size * price) / leverage;
    }

    private void updateEquity(Account account) {
        double unrealizedProfit = calculateUnrealizedProfit(account);
        account.setEquity(account.getBalance() + unrealizedProfit);
        account.setFreeMargin(account.getEquity() - account.getMarginUsed());
    }

    private double calculateProfit(Trade trade, double closingPrice) {
        double size = trade.getSize();
        double openingPrice = trade.getOpeningPrice();
        String type = trade.getType();

        if ("BUY".equalsIgnoreCase(type)) {
            return (closingPrice - openingPrice) * size;
        } else if ("SELL".equalsIgnoreCase(type)) {
            return (openingPrice - closingPrice) * size;
        }
        return 0.0;
    }

    private String generateUniqueTradeNo() {
        return "TR" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void sendAccountUpdate(Account account) {
        AccountUpdateDto update = new AccountUpdateDto(
            account.getId(),
            account.getBalance(),
            account.getEquity(),
            account.getMarginUsed(),
            account.getFreeMargin(),
            System.currentTimeMillis()
        );

        messagingTemplate.convertAndSendToUser(
            account.getUsername(),
            "/queue/account",
            update
        );
    }
}  
