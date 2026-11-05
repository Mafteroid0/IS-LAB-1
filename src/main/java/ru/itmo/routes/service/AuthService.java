package ru.itmo.routes.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.routes.dto.Requests.Credentials;
import ru.itmo.routes.error.ConflictException;
import ru.itmo.routes.model.Account;
import ru.itmo.routes.repository.AccountRepository;

/** Регистрация учётных записей; вход выполняет Spring Security (AuthenticationManager). */
@Service
@Transactional
public class AuthService {
    private final AccountRepository accounts;
    private final WriteCoordinator changes;
    private final PasswordEncoder encoder;

    public AuthService(AccountRepository accounts, WriteCoordinator changes, PasswordEncoder encoder) {
        this.accounts = accounts;
        this.changes = changes;
        this.encoder = encoder;
    }

    public String register(Credentials input) {
        String hash = encoder.encode(input.password);
        changes.changed();
        if (accounts.findByUsername(input.username).isPresent()) throw new ConflictException("Логин уже занят");
        accounts.saveAndFlush(new Account(input.username, hash));
        return input.username;
    }
}
