package ru.itmo.routes.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.routes.model.State;

/** Сериализует записи и увеличивает ревизию данных, по которой клиент узнаёт об изменениях. */
@Service
public class WriteCoordinator {
    @PersistenceContext
    private EntityManager em;

    @Transactional(propagation = Propagation.MANDATORY)
    public void changed() {
        em.find(State.class, 1, LockModeType.PESSIMISTIC_WRITE).advance();
    }

    @Transactional(readOnly = true)
    public String revision() {
        return Long.toString(em.find(State.class, 1).getRevision());
    }
}
