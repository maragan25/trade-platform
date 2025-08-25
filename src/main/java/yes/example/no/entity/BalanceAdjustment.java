package yes.example.no.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class BalanceAdjustment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    private double adjustmentAmount;
    private double previousBalance;
    private double newBalance;
    private String reason;
    private String adjustedBy; // Admin username
    private LocalDateTime adjustmentTime = LocalDateTime.now();
    private String notes;
}
