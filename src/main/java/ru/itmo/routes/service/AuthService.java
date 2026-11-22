package ru.itmo.routes.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.routes.dto.Requests.Credentials;
import ru.itmo.routes.error.ConflictException;
import ru.itmo.routes.model.Account;
import ru.itmo.routes.repository.AccountRepository;
import ru.itmo.routes.security.Roles;

/** Регистрация учётных записей; вход выполняет Spring Security (AuthenticationManager). */
@Service
@Transactional(isolation = Isolation.READ_COMMITTED)
public class AuthService {
    private final AccountRepository accounts;
    private final WriteCoordinator changes;
    private final PasswordEncoder encoder;
    private final Roles roles;

    public AuthService(AccountRepository accounts, WriteCoordinator changes, PasswordEncoder encoder, Roles roles) {
        this.accounts = accounts;
        this.changes = changes;
        this.encoder = encoder;
        this.roles = roles;
    }

    public String register(Credentials input) {
        if (roles.isAdminLogin(input.username)) throw new ConflictException("Логин недоступен для регистрации");
        String hash = encoder.encode(input.password);
        changes.changed();
        if (accounts.findByUsername(input.username).isPresent()) throw new ConflictException("Логин уже занят");
        accounts.saveAndFlush(new Account(input.username, hash));
        return input.username;
    }
}
