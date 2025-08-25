package yes.example.no.controller;

import yes.example.no.entity.Account;
import yes.example.no.entity.Transaction;
import yes.example.no.repository.AccountRepository;
import yes.example.no.repository.TransactionRepository;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountRepository accountRepo;
    private final TransactionRepository transactionRepository;


    public AccountController(AccountRepository accountRepo, TransactionRepository transactionRepository) {
        this.accountRepo = accountRepo;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping
    public List<Account> getAllAccounts() {
        return accountRepo.findAll();
    }

    @PostMapping
    public Account createAccount(@RequestBody Account account) {
        return accountRepo.save(account);
    }

    @PostMapping("/{id}/deposit")
    public Account deposit(@PathVariable Long id, @RequestParam double amount) {
        Account account = accountRepo.findById(id).orElseThrow();
        account.setBalance(account.getBalance() + amount);

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setAmount(amount);
        transaction.setType("DEPOSIT");

        accountRepo.save(account);
        transactionRepository.save(transaction);

        return account;
    }

    @PostMapping("/{id}/withdraw")
    public Account withdraw(@PathVariable Long id, @RequestParam double amount) {
        Account account = accountRepo.findById(id).orElseThrow();

        if (account.getBalance() < amount) {
            throw new RuntimeException("Insufficient funds");
        }

        account.setBalance(account.getBalance() - amount);

        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setAmount(-amount);
        transaction.setType("WITHDRAW");

        accountRepo.save(account);
        transactionRepository.save(transaction);

        return account;
    }


}