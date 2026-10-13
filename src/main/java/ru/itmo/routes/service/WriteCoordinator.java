package ru.itmo.routes.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import ru.itmo.routes.model.State;

@ApplicationScoped
public class WriteCoordinator {
    @PersistenceContext(unitName = "routes")
    private EntityManager em;

    @Transactional(Transactional.TxType.MANDATORY)
    public void changed() {
        em.find(State.class, 1, LockModeType.PESSIMISTIC_WRITE).advance();
    }

    @Transactional
    public String revision() {
        return Long.toString(em.find(State.class, 1).getRevision());
    }
}
