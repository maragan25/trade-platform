package yes.example.no.service;

import yes.example.no.entity.*;
import yes.example.no.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BalanceReconciliationService {
    
    private final AccountRepository accountRepo;
    private final TransactionRepository transactionRepo;
    private final TradeRepository tradeRepo;
    private final BalanceAdjustmentRepository balanceAdjustmentRepo;

    public class ReconciliationResult {
        public double calculatedBalance;
        public double currentBalance;
        public double difference;
        public List<String> issues;
        
        public ReconciliationResult(double calculated, double current, List<String> issues) {
            this.calculatedBalance = calculated;
            this.currentBalance = current;
            this.difference = calculated - current;
            this.issues = issues;
        }
    }

    public ReconciliationResult reconcileAccount(Long accountId) {
        Account account = accountRepo.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        // Get all transactions
        List<Transaction> transactions = transactionRepo.findByAccountOrderByTimestamp(account);
        
        // Get all closed trades
        List<Trade> closedTrades = tradeRepo.findByAccountAndOpenFalseOrderByCloseTime(account);
        
        // Get all balance adjustments
        List<BalanceAdjustment> adjustments = balanceAdjustmentRepo.findByAccountOrderByAdjustmentTime(account);

        double calculatedBalance = 0.0;
        List<String> issues = new java.util.ArrayList<>();

        // Add all deposits and withdrawals
        for (Transaction transaction : transactions) {
            calculatedBalance += transaction.getAmount();
        }

        // Add realized profits from closed trades
        for (Trade trade : closedTrades) {
            if (trade.getRealizedProfit() != null) {
                calculatedBalance += trade.getRealizedProfit();
            } else {
                // Calculate profit if not stored
                if (trade.getClosingPrice() != null) {
                    double profit = calculateTradeProfit(trade, trade.getClosingPrice());
                    calculatedBalance += profit;
                    issues.add("Trade " + trade.getUniqueTradeNo() + " missing realized profit - calculated: " + profit);
                }
            }
            
            // Subtract commission and swap
            calculatedBalance -= (trade.getCommission() + trade.getSwap());
        }

        // Add balance adjustments
        for (BalanceAdjustment adjustment : adjustments) {
            calculatedBalance += adjustment.getAdjustmentAmount();
        }

        return new ReconciliationResult(calculatedBalance, account.getBalance(), issues);
    }

    public void fixAccountBalance(Long accountId, String reason, String adminUsername) {
        ReconciliationResult result = reconcileAccount(accountId);
        
        if (Math.abs(result.difference) > 0.01) { // Threshold for adjustment
            Account account = accountRepo.findById(accountId).orElseThrow();
            
            BalanceAdjustment adjustment = new BalanceAdjustment();
            adjustment.setAccount(account);
            adjustment.setAdjustmentAmount(result.difference);
            adjustment.setPreviousBalance(account.getBalance());
            adjustment.setNewBalance(result.calculatedBalance);
            adjustment.setReason(reason);
            adjustment.setAdjustedBy(adminUsername);
            adjustment.setNotes("Auto-correction from reconciliation. Issues: " + 
                              String.join(", ", result.issues));
            
            balanceAdjustmentRepo.save(adjustment);
            
            account.setBalance(result.calculatedBalance);
            accountRepo.save(account);
        }
    }

    private double calculateTradeProfit(Trade trade, double closingPrice) {
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
}
