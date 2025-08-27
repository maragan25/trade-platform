package yes.example.no.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "watchlist", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"account_id", "symbol_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Watchlist {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "symbol_id", nullable = false)
    private Symbol symbol;
    
    @Column(name = "order_index")
    private Integer orderIndex = 0;
    
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();
    
    public Watchlist(Account account, Symbol symbol) {
        this.account = account;
        this.symbol = symbol;
        this.createdAt = java.time.LocalDateTime.now();
    }
}