package ru.itmo.routes.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;
import ru.itmo.routes.model.*;
import java.util.List;

@ApplicationScoped
public class ReferenceRepository {
    @PersistenceContext(unitName = "routes")
    private EntityManager em;

    public Coordinates coordinates(long id) {
        return em.find(Coordinates.class, id);
    }

    public Location location(long id) {
        return em.find(Location.class, id);
    }

    public List<Coordinates> coordinates() {
        return em.createQuery("select c from Coordinates c order by c.id", Coordinates.class).getResultList();
    }

    public List<Location> locations() {
        return em.createQuery("select l from Location l order by l.name, l.id", Location.class).getResultList();
    }

    public void save(Object value) {
        em.persist(value);
    }

    public void remove(Object value) {
        em.remove(value);
    }

    public void flush() {
        em.flush();
    }
}
