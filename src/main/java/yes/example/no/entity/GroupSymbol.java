package yes.example.no.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "group_symbols")
public class GroupSymbol {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "group_id")
    private Group group;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "symbol_id")
    private Symbol symbol;

    private boolean canViewQuotes = true;
    private boolean canTrade = true;
    
    // Markup/spread adjustments
    private double bidMarkup = 0.0;    // Added to bid price
    private double askMarkup = 0.0;    // Added to ask price
    
    // Percentage markups (alternative to fixed)
    private double bidMarkupPercent = 0.0;
    private double askMarkupPercent = 0.0;
}