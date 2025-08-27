package yes.example.no.controller;

import yes.example.no.dto.LoginRequest;
import yes.example.no.dto.LoginResponse;
import yes.example.no.entity.Account;
import yes.example.no.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AccountRepository accountRepo;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest, HttpSession session) {
        Optional<Account> accountOpt = accountRepo.findByUsername(loginRequest.getUsername());
        
        if (accountOpt.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Account account = accountOpt.get();
        
        if (!passwordEncoder.matches(loginRequest.getPassword(), account.getPassword())) {
            return ResponseEntity.badRequest().build();
        }

        // Store user in session
        session.setAttribute("userId", account.getId());
        session.setAttribute("username", account.getUsername());
        session.setAttribute("isAdmin", account.isAdmin());

        LoginResponse response = new LoginResponse();
        response.setAccount(account);
        response.setToken("session-authenticated");
        
        session.setAttribute("username", account.getUsername());
        session.setAttribute("isAdmin", account.isAdmin());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/current")
    public ResponseEntity<Account> getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<Account> account = accountRepo.findById(userId);
        if (account.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(account.get());
    }
}