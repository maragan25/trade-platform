package yes.example.no.config;

import yes.example.no.entity.*;
import yes.example.no.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnhancedDataInitializer implements CommandLineRunner {
    
    private final AccountRepository accountRepo;
    private final SymbolRepository symbolRepo;
    private final GroupRepository groupRepo;
    private final GroupSymbolRepository groupSymbolRepo;
    private final WatchlistRepository watchlistRepo;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        // Create symbols first
        createSymbols();
        
        // Create basic groups only if none exist - let admin create more via UI
        createBasicGroups();
        
        // Create accounts
        createAccounts();
        
        System.out.println("=== Data Initialization Complete ===");
        System.out.println("Admin user: admin/admin123");
        System.out.println("Test users: user1/password123, user2/password123, vipuser/vip123");
        System.out.println("Groups can be managed via admin panel");
    }
    
    private void createSymbols() {
        if (symbolRepo.count() == 0) {
            symbolRepo.save(createSymbol("EURUSD", 1.08500, 1.08520));
            symbolRepo.save(createSymbol("GBPUSD", 1.25300, 1.25320));
            symbolRepo.save(createSymbol("USDJPY", 149.450, 149.470));
            symbolRepo.save(createSymbol("USDCHF", 0.90200, 0.90220));
            symbolRepo.save(createSymbol("AUDUSD", 0.67800, 0.67820));
            symbolRepo.save(createSymbol("USDCAD", 1.36200, 1.36220));
            symbolRepo.save(createSymbol("NZDUSD", 0.62100, 0.62120));
            symbolRepo.save(createSymbol("EURGBP", 0.86700, 0.86720));
            symbolRepo.save(createSymbol("EURJPY", 162.150, 162.170));
            symbolRepo.save(createSymbol("GBPJPY", 187.250, 187.270));
            symbolRepo.save(createSymbol("XAUUSD", 2025.50, 2026.00));
            symbolRepo.save(createSymbol("XAGUSD", 24.150, 24.200));
            symbolRepo.save(createSymbol("USOIL", 78.250, 78.350));
            System.out.println("Created " + symbolRepo.count() + " symbols");
        }
    }
    
    private void createBasicGroups() {
        // Only create one default group if no groups exist
        if (groupRepo.count() == 0) {
            Group defaultGroup = new Group();
            defaultGroup.setName("Default");
            defaultGroup.setDescription("Default group with basic forex pairs");
            defaultGroup.setActive(true);
            groupRepo.save(defaultGroup);
            
            System.out.println("Created default group - admin can create more groups via admin panel");
        }
    }
    
    private void createAccounts() {
        // Create admin account
        if (accountRepo.findByUsername("admin").isEmpty()) {
            Account admin = new Account();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setBalance(50000.0);
            admin.setEquity(50000.0);
            admin.setFreeMargin(50000.0);
            admin.setAdmin(true);
            // Admin doesn't need a group
            accountRepo.save(admin);
            System.out.println("Created admin user: admin/admin123");
        }
        
        // Create test users without hardcoded groups - let admin assign them
        Group defaultGroup = groupRepo.findByName("Default").orElse(null);
        
        if (accountRepo.findByUsername("user1").isEmpty()) {
            Account user1 = new Account();
            user1.setUsername("user1");
            user1.setPassword(passwordEncoder.encode("password123"));
            user1.setBalance(10000.0);
            user1.setEquity(10000.0);
            user1.setFreeMargin(10000.0);
            // Don't assign group by default - let admin do it
            accountRepo.save(user1);
            System.out.println("Created user: user1/password123 (no group assigned)");
        }
        
        if (accountRepo.findByUsername("user2").isEmpty()) {
            Account user2 = new Account();
            user2.setUsername("user2");
            user2.setPassword(passwordEncoder.encode("password123"));
            user2.setBalance(15000.0);
            user2.setEquity(15000.0);
            user2.setFreeMargin(15000.0);
            // Don't assign group by default - let admin do it
            accountRepo.save(user2);
            System.out.println("Created user: user2/password123 (no group assigned)");
        }
        
        if (accountRepo.findByUsername("vipuser").isEmpty()) {
            Account vipUser = new Account();
            vipUser.setUsername("vipuser");
            vipUser.setPassword(passwordEncoder.encode("vip123"));
            vipUser.setBalance(25000.0);
            vipUser.setEquity(25000.0);
            vipUser.setFreeMargin(25000.0);
            // Don't assign group by default - let admin do it
            accountRepo.save(vipUser);
            System.out.println("Created user: vipuser/vip123 (no group assigned)");
        }
    }
    
    private Symbol createSymbol(String name, double bid, double ask) {
        Symbol symbol = new Symbol();
        symbol.setName(name);
        symbol.setBidPrice(bid);
        symbol.setAskPrice(ask);
        symbol.setActive(true);
        return symbol;
    }
}