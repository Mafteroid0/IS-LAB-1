package ru.itmo.routes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.routes.model.Account;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUsername(String username);
}
