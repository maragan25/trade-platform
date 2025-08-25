package yes.example.no.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Set;

@Entity
@Getter
@Setter
public class Symbol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;

    @Column(name = "bid_price", nullable = false)
    private double bidPrice;

    @Column(name = "ask_price", nullable = false)
    private double askPrice;

    private boolean active = true; // can be traded or not
    
    @OneToMany(mappedBy = "symbol", fetch = FetchType.LAZY)
    @JsonIgnore // Prevent circular reference in JSON
    private Set<GroupSymbol> groupSymbols;
    
    @OneToMany(mappedBy = "symbol", fetch = FetchType.LAZY)
    @JsonIgnore // Prevent circular reference in JSON
    private Set<Watchlist> watchlists;
}