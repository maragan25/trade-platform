package yes.example.no.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Set;

@Entity
@Getter
@Setter
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String username;
    
    @JsonIgnore // Hide password in JSON responses
    private String password;
    
    private double balance;
    private boolean isAdmin = false;

    // Trading-related fields
    private double equity = 0.0;
    private double marginUsed = 0.0;
    private double freeMargin = 0.0;

    // Group relationship - prevent circular reference
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "group_id")
    private Group group;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore // Prevent circular reference in JSON
    private Set<Watchlist> watchlists;

    @PrePersist
    @PreUpdate
    private void updateMargins() {
        if (equity <= 0) {
            equity = balance;
        }
        freeMargin = equity - marginUsed;
    }
}