package yes.example.no.controller;

import yes.example.no.dto.LoginRequest;
import yes.example.no.entity.Account;
import yes.example.no.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class LoginController {

    private final AccountRepository accountRepo;
    private final PasswordEncoder passwordEncoder;

    // Simple DTO to avoid circular references
    public static class AccountDto {
        private Long id;
        private String username;
        private double balance;
        private double equity;
        private double marginUsed;
        private double freeMargin;
        private boolean admin;
        private String groupName;
        
        public AccountDto(Account account) {
            this.id = account.getId();
            this.username = account.getUsername();
            this.balance = account.getBalance();
            this.equity = account.getEquity();
            this.marginUsed = account.getMarginUsed();
            this.freeMargin = account.getFreeMargin();
            this.admin = account.isAdmin();
            this.groupName = account.getGroup() != null ? account.getGroup().getName() : null;
        }
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public double getBalance() { return balance; }
        public void setBalance(double balance) { this.balance = balance; }
        public double getEquity() { return equity; }
        public void setEquity(double equity) { this.equity = equity; }
        public double getMarginUsed() { return marginUsed; }
        public void setMarginUsed(double marginUsed) { this.marginUsed = marginUsed; }
        public double getFreeMargin() { return freeMargin; }
        public void setFreeMargin(double freeMargin) { this.freeMargin = freeMargin; }
        public boolean isAdmin() { return admin; }
        public void setAdmin(boolean admin) { this.admin = admin; }
        public String getGroupName() { return groupName; }
        public void setGroupName(String groupName) { this.groupName = groupName; }
    }

    public static class LoginResponseDto {
        private AccountDto account;
        private String token;
        
        public LoginResponseDto(AccountDto account, String token) {
            this.account = account;
            this.token = token;
        }
        
        // Getters and setters
        public AccountDto getAccount() { return account; }
        public void setAccount(AccountDto account) { this.account = account; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequest loginRequest, 
                                                  HttpServletRequest request) {
        try {
            System.out.println("Login attempt for user: " + loginRequest.getUsername());
            
            Optional<Account> accountOpt = accountRepo.findByUsername(loginRequest.getUsername());
            if (accountOpt.isEmpty()) {
                System.out.println("Login failed: User not found - " + loginRequest.getUsername());
                return ResponseEntity.status(401).build();
            }
    
            Account account = accountOpt.get();
            if (!passwordEncoder.matches(loginRequest.getPassword(), account.getPassword())) {
                System.out.println("Login failed: Invalid password for user - " + loginRequest.getUsername());
                return ResponseEntity.status(401).build();
            }
    
            // --- Spring Security integration ---
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            if (account.isAdmin()) authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
    
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(account.getUsername(), null, authorities);
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
    
            // --- Store session attributes ---
            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            session.setAttribute("userId", account.getId());
            session.setAttribute("username", account.getUsername());
            session.setAttribute("isAdmin", account.isAdmin());
            session.setMaxInactiveInterval(30 * 60); // 30 min timeout
    
            System.out.println("Login successful for user: " + account.getUsername() + 
                             " (Admin: " + account.isAdmin() + ") Session: " + session.getId());
    
            // --- Return DTO for frontend ---
            AccountDto accountDto = new AccountDto(account);
            LoginResponseDto response = new LoginResponseDto(accountDto, "Login successful");
    
            return ResponseEntity.ok(response);
    
        } catch (Exception e) {
            System.err.println("Login error for user " + loginRequest.getUsername() + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
    

    @PostMapping("/register")
    public ResponseEntity<AccountDto> register(@RequestBody Account account) {
        try {
            if (accountRepo.findByUsername(account.getUsername()).isPresent()) {
                System.out.println("Registration failed: Username already exists - " + account.getUsername());
                return ResponseEntity.badRequest().build();
            }

            account.setPassword(passwordEncoder.encode(account.getPassword()));
            
            // Set default values
            if (account.getBalance() <= 0) {
                account.setBalance(10000.0);
            }
            if (account.getEquity() <= 0) {
                account.setEquity(account.getBalance());
            }
            if (account.getFreeMargin() <= 0) {
                account.setFreeMargin(account.getBalance());
            }
            
            // Don't assign admin status during registration
            account.setAdmin(false);

            Account saved = accountRepo.save(account);
            System.out.println("New account registered: " + saved.getUsername());
            
            AccountDto accountDto = new AccountDto(saved);
            return ResponseEntity.ok(accountDto);
            
        } catch (Exception e) {
            System.err.println("Registration error for user " + account.getUsername() + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                String username = (String) session.getAttribute("username");
                System.out.println("Logout for user: " + username);
                session.invalidate();
            }
            SecurityContextHolder.clearContext();
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("Logout error: " + e.getMessage());
            return ResponseEntity.ok().build(); // Still return ok even if error
        }
    }

    @GetMapping("/current")
    public ResponseEntity<AccountDto> getCurrentUser(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                return ResponseEntity.status(401).build();
            }

            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(401).build();
            }

            Optional<Account> accountOpt = accountRepo.findById(userId);
            if (accountOpt.isEmpty()) {
                return ResponseEntity.status(401).build();
            }

            Account account = accountOpt.get();
            AccountDto accountDto = new AccountDto(account);
            return ResponseEntity.ok(accountDto);
            
        } catch (Exception e) {
            System.err.println("Get current user error: " + e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }
}