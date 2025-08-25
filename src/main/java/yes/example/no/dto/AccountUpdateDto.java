package yes.example.no.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountUpdateDto {
    private Long accountId;
    private double balance;
    private double equity;
    private double marginUsed;
    private double freeMargin;
    private long timestamp;
}