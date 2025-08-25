package yes.example.no.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TradingSummaryDto {
    private Long accountId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private int totalTrades;
    private int winningTrades;
    private int losingTrades;
    private double winRate;
    private double totalProfit;
    private double largestWin;
    private double largestLoss;
    private double averageWin;
    private double averageLoss;
    private double profitFactor;
}