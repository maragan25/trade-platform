package yes.example.no.controller;

import yes.example.no.entity.Symbol;
import yes.example.no.repository.SymbolRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/symbols")
public class SymbolController {

    private final SymbolRepository symbolRepo;


    public SymbolController(SymbolRepository symbolRepo) {
        this.symbolRepo = symbolRepo;
    }

    private Symbol createDemoSymbol(String name, double bid, double ask) {
        Symbol symbol = new Symbol();
        symbol.setName(name);
        symbol.setBidPrice(bid);
        symbol.setAskPrice(ask);
        symbol.setActive(true);
        return symbol;
    }

    @PostMapping("/init-demo-data")
    public ResponseEntity<String> initDemoData() {
    try {
        // Create symbols if they don't exist
        if (symbolRepo.count() == 0) {
            symbolRepo.save(createDemoSymbol("EURUSD", 1.08500, 1.08520));
            symbolRepo.save(createDemoSymbol("GBPUSD", 1.25300, 1.25320));
            symbolRepo.save(createDemoSymbol("USDJPY", 149.450, 149.470));
            symbolRepo.save(createDemoSymbol("AUDUSD", 0.67800, 0.67820));
            symbolRepo.save(createDemoSymbol("USDCAD", 1.36200, 1.36220));
            symbolRepo.save(createDemoSymbol("EURGBP", 0.86450, 0.86470));
            symbolRepo.save(createDemoSymbol("EURJPY", 162.250, 162.270));
            symbolRepo.save(createDemoSymbol("GBPJPY", 187.890, 187.910));
            symbolRepo.save(createDemoSymbol("AUDJPY", 101.320, 101.340));
            symbolRepo.save(createDemoSymbol("NZDUSD", 0.61200, 0.61220));
            
            return ResponseEntity.ok("Demo symbols created successfully! Total: " + symbolRepo.count());
        } else {
            return ResponseEntity.ok("Symbols already exist. Count: " + symbolRepo.count());
        }
    } catch (Exception e) {
        return ResponseEntity.badRequest().body("Error creating symbols: " + e.getMessage());
    }
}

    @GetMapping
    public List<Symbol> getAllSymbols() {
        return symbolRepo.findAll();
    }

    @PostMapping
    public Symbol createSymbol(@RequestBody Symbol symbol) {
        return symbolRepo.save(symbol);
    }

    @PutMapping("/{id}")
    public Symbol updateSymbol(@PathVariable Long id, @RequestBody Symbol updated) {
        Symbol symbol = symbolRepo.findById(id).orElseThrow();
        symbol.setName(updated.getName());
        symbol.setBidPrice(updated.getBidPrice());
        symbol.setAskPrice(updated.getAskPrice());
        symbol.setActive(updated.isActive());
        return symbolRepo.save(symbol);
    }

    @DeleteMapping("/{id}")
    public void deleteSymbol(@PathVariable Long id) {
        symbolRepo.deleteById(id);
    }
}