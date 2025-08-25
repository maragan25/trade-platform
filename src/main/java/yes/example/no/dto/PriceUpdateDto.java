package yes.example.no.dto;

import lombok.Data;

@Data
public class PriceUpdateDto {
    private Long symbolId;
    private double bidPrice;
    private double askPrice;
    private long timestamp;
}