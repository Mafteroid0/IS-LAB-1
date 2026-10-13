package ru.itmo.routes.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ru.itmo.routes.model.Account;
import ru.itmo.routes.repository.AccountRepository;
import ru.itmo.routes.dto.Requests.Credentials;
import ru.itmo.routes.error.Problem;
import static ru.itmo.routes.error.Problem.Kind.*;

@ApplicationScoped
@Transactional
public class AuthService {
    @Inject private AccountRepository accounts;
    @Inject private WriteCoordinator changes;

    public String register(Credentials input) {
        String hash = Passwords.hash(input.password);
        changes.changed();
        if (accounts.find(input.username).isPresent()) throw new Problem(CONFLICT, "Логин уже занят");
        accounts.save(new Account(input.username, hash));
        return input.username;
    }

    public String login(Credentials input) {
        Account account = accounts.find(input.username)
                .orElseThrow(() -> new Problem(UNAUTHORIZED, "Неверный логин или пароль"));
        if (!Passwords.matches(input.password, account.getPasswordHash()))
            throw new Problem(UNAUTHORIZED, "Неверный логин или пароль");
        return account.getUsername();
    }
}
