package yes.example.no.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SymbolPriceDto {
    private Long symbolId;
    private String symbolName;
    private double bidPrice;
    private double askPrice;
    private long timestamp;
}