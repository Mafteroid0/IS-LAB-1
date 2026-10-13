package ru.itmo.routes.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;
import ru.itmo.routes.model.Account;
import java.util.Optional;

@ApplicationScoped
public class AccountRepository {
    @PersistenceContext(unitName = "routes")
    private EntityManager em;

    public Optional<Account> find(String username) {
        return em.createQuery("select a from Account a where a.username = :username", Account.class)
                .setParameter("username", username).getResultStream().findFirst();
    }

    public void save(Account account) {
        em.persist(account);
        em.flush();
    }
}
