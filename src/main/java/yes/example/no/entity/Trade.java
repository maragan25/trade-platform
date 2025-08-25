package yes.example.no.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String uniqueTradeNo;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne
    @JoinColumn(name = "symbol_id")
    private Symbol symbol;

    private String type; // "BUY" or "SELL"
    private double size;
    private double openingPrice;
    private Double closingPrice;
    private boolean open = true;
    
    private LocalDateTime openTime = LocalDateTime.now();
    private LocalDateTime closeTime;
    
    private double margin; // Margin used by this trade
    private Double realizedProfit; // Set when trade is closed
    
    // Commission/swap fields
    private double commission = 0.0;
    private double swap = 0.0;
    
    // Calculate unrealized profit for open trades
    public double calculateUnrealizedProfit(double currentPrice) {
        if (!open || closingPrice != null) {
            return realizedProfit != null ? realizedProfit : 0.0;
        }
        
        if ("BUY".equalsIgnoreCase(type)) {
            return (currentPrice - openingPrice) * size;
        } else if ("SELL".equalsIgnoreCase(type)) {
            return (openingPrice - currentPrice) * size;
        }
        return 0.0;
    }
}
