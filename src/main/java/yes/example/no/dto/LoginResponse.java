package yes.example.no.dto;

import yes.example.no.entity.Account;
import lombok.Data;

@Data
public class LoginResponse {
    private Account account;
    private String token;
}