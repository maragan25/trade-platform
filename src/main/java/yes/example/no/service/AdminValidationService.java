package yes.example.no.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import yes.example.no.entity.Account;
import yes.example.no.repository.AccountRepository;

@Component
@RequiredArgsConstructor
public class AdminValidationService {
    
    private final AccountRepository accountRepo;
    
    public Account validateAdmin(HttpServletRequest request) {
        // Check session for logged-in user
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No session found");
        }
        
        String username = (String) session.getAttribute("username");
        if (username == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        
        Account account = accountRepo.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));
        
        if (!account.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
        
        return account;
    }
}
