package yes.example.no.service;

import yes.example.no.entity.PriceHistory;
import yes.example.no.entity.Symbol;
import yes.example.no.repository.PriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PriceService {
    
    private final PriceHistoryRepository priceHistoryRepo;

    public void savePriceHistory(Symbol symbol, double bidPrice, double askPrice) {
        PriceHistory priceHistory = new PriceHistory();
        priceHistory.setSymbol(symbol);
        priceHistory.setBidPrice(bidPrice);
        priceHistory.setAskPrice(askPrice);
        priceHistory.setTimestamp(LocalDateTime.now());
        
        priceHistoryRepo.save(priceHistory);
    }
}
