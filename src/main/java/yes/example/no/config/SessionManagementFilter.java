package yes.example.no.config;

import yes.example.no.entity.Account;
import yes.example.no.repository.AccountRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SessionManagementFilter implements Filter {

    private final AccountRepository accountRepo;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Skip authentication for public endpoints
        String requestURI = httpRequest.getRequestURI();
        if (isPublicEndpoint(requestURI)) {
            chain.doFilter(request, response);
            return;
        }

        // Only process if no authentication exists
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            HttpSession session = httpRequest.getSession(false);
            
            if (session != null) {
                String username = (String) session.getAttribute("username");
                Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
                
                if (username != null) {
                    try {
                        // Verify user still exists and get fresh data
                        Account account = accountRepo.findByUsername(username).orElse(null);
                        if (account != null) {
                            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                            if (account.isAdmin()) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                            }
                            
                            UsernamePasswordAuthenticationToken auth = 
                                new UsernamePasswordAuthenticationToken(username, null, authorities);
                            SecurityContextHolder.getContext().setAuthentication(auth);
                            
                            // Update session with fresh admin status
                            session.setAttribute("isAdmin", account.isAdmin());
                        } else {
                            // User no longer exists, invalidate session
                            session.invalidate();
                        }
                    } catch (Exception e) {
                        System.err.println("Error in session management: " + e.getMessage());
                        // Continue without authentication
                    }
                }
            }
        }
        
        chain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String uri) {
        return uri.startsWith("/ws") ||
               uri.equals("/api/accounts/login") ||
               uri.equals("/api/accounts/register") ||
               uri.startsWith("/api/auth") ||
               uri.startsWith("/static") ||
               uri.equals("/") ||
               uri.equals("/index.html") ||
               uri.equals("/admin.html") ||
               uri.equals("/favicon.ico") ||
               uri.startsWith("/css") ||
               uri.startsWith("/js") ||
               uri.startsWith("/images") ||
               uri.contains("error");
    }
}