package yes.example.no.dto;

import lombok.*;

@Getter
@Setter
public class TradeRequest {
    private Long accountId;
    private Long symbolId;
    private String type;
    private Double size;
    private Double openingPrice;
}