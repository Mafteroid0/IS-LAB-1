package ru.itmo.routes.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.itmo.routes.repository.AccountRepository;

@Service
public class AccountUserDetailsService implements UserDetailsService {
    private final AccountRepository accounts;
    private final Roles roles;

    public AccountUserDetailsService(AccountRepository accounts, Roles roles) {
        this.accounts = accounts;
        this.roles = roles;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return accounts.findByUsername(username)
                .map(account -> User.withUsername(account.getUsername())
                        .password(account.getPasswordHash()).authorities(roles.authorities(account.getUsername())).build())
                .orElseThrow(() -> new UsernameNotFoundException("Неверный логин или пароль"));
    }
}
