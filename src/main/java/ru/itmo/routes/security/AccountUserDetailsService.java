package ru.itmo.routes.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.itmo.routes.repository.AccountRepository;
import java.util.List;

@Service
public class AccountUserDetailsService implements UserDetailsService {
    private final AccountRepository accounts;

    public AccountUserDetailsService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return accounts.findByUsername(username)
                .map(account -> User.withUsername(account.getUsername())
                        .password(account.getPasswordHash()).authorities(List.of()).build())
                .orElseThrow(() -> new UsernameNotFoundException("Неверный логин или пароль"));
    }
}
