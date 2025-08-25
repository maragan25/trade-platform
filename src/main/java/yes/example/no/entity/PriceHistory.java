package yes.example.no.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(indexes = {
    @Index(name = "idx_symbol_timestamp", columnList = "symbol_id, timestamp"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
public class PriceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "symbol_id")
    private Symbol symbol;

    private LocalDateTime timestamp;
    private double bidPrice;
    private double askPrice;
    private double volume = 0.0; // Optional
}
    