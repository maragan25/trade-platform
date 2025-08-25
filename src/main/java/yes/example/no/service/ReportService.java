package yes.example.no.service;

import yes.example.no.dto.TradingSummaryDto;
import yes.example.no.entity.Account;
import yes.example.no.entity.Trade;
import yes.example.no.repository.AccountRepository;
import yes.example.no.repository.TradeRepository;
import yes.example.no.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {
    
    private final AccountRepository accountRepo;
    private final TradeRepository tradeRepo;
    private final TransactionRepository transactionRepo;

    public TradingSummaryDto generateTradingSummary(Long accountId, LocalDate startDate, LocalDate endDate) {
        Account account = accountRepo.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDateTime.now().minusMonths(1);
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : LocalDateTime.now();

        List<Trade> trades = tradeRepo.findByAccountAndOpenTimeBetween(account, start, end);
        List<Trade> closedTrades = trades.stream()
                .filter(trade -> !trade.isOpen() && trade.getRealizedProfit() != null)
                .toList();

        TradingSummaryDto summary = new TradingSummaryDto();
        summary.setAccountId(accountId);
        summary.setStartDate(start);
        summary.setEndDate(end);
        summary.setTotalTrades(closedTrades.size());

        if (!closedTrades.isEmpty()) {
            // Calculate statistics
            double totalProfit = closedTrades.stream()
                    .mapToDouble(Trade::getRealizedProfit)
                    .sum();

            long winningTrades = closedTrades.stream()
                    .mapToLong(trade -> trade.getRealizedProfit() > 0 ? 1 : 0)
                    .sum();

            long losingTrades = closedTrades.size() - winningTrades;

            double largestWin = closedTrades.stream()
                    .mapToDouble(Trade::getRealizedProfit)
                    .max()
                    .orElse(0.0);

            double largestLoss = closedTrades.stream()
                    .mapToDouble(Trade::getRealizedProfit)
                    .min()
                    .orElse(0.0);

            double averageWin = closedTrades.stream()
                    .filter(trade -> trade.getRealizedProfit() > 0)
                    .mapToDouble(Trade::getRealizedProfit)
                    .average()
                    .orElse(0.0);

            double averageLoss = closedTrades.stream()
                    .filter(trade -> trade.getRealizedProfit() < 0)
                    .mapToDouble(Trade::getRealizedProfit)
                    .average()
                    .orElse(0.0);

            summary.setTotalProfit(totalProfit);
            summary.setWinningTrades((int) winningTrades);
            summary.setLosingTrades((int) losingTrades);
            summary.setWinRate(totalProfit > 0 ? (double) winningTrades / closedTrades.size() * 100 : 0);
            summary.setLargestWin(largestWin);
            summary.setLargestLoss(largestLoss);
            summary.setAverageWin(averageWin);
            summary.setAverageLoss(averageLoss);
            summary.setProfitFactor(averageLoss != 0 ? Math.abs(averageWin / averageLoss) : 0);
        }

        return summary;
    }

    public TradingSummaryDto generateMonthlyReport(Long accountId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        return generateTradingSummary(accountId, startDate, endDate);
    }
}
