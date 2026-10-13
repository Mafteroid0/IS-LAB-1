package ru.itmo.routes.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;
import ru.itmo.routes.model.Route;
import java.util.List;

@ApplicationScoped
public class RouteRepository {
    @PersistenceContext(unitName = "routes")
    private EntityManager em;

    public Route find(long id) {
        return em.find(Route.class, id);
    }

    public void save(Route route) {
        em.persist(route);
    }

    public void remove(Route route) {
        em.remove(route);
    }

    public void flush() {
        em.flush();
    }

    public List<Route> list(String name, String fromName, String toName, String sort,
                            boolean descending, int offset, int limit) {
        String column = switch (sort) {
            case "id" -> "r.id";
            case "name" -> "r.name";
            case "fromName" -> "f.name";
            case "toName" -> "t.name";
            case "creationDate" -> "r.creationDate";
            case "distance" -> "r.distance";
            case "rating" -> "r.rating";
            default -> throw new IllegalArgumentException("Недопустимая сортировка");
        };
        TypedQuery<Route> query = em.createQuery("select r from Route r join fetch r.coordinates "
                + "left join fetch r.from f join fetch r.to t " + filter()
                + " order by " + column + (descending ? " desc" : " asc") + " nulls last, r.id", Route.class);
        parameters(query, name, fromName, toName);
        return query.setFirstResult(offset).setMaxResults(limit).getResultList();
    }

    public long count(String name, String fromName, String toName) {
        TypedQuery<Long> query = em.createQuery("select count(r) from Route r left join r.from f join r.to t "
                + filter(), Long.class);
        parameters(query, name, fromName, toName);
        return query.getSingleResult();
    }

    public List<Route> byCoordinates(long id) {
        return em.createQuery("select r from Route r where r.coordinates.id = :id", Route.class)
                .setParameter("id", id).getResultList();
    }

    public List<Route> byLocation(long id) {
        return em.createQuery("select r from Route r left join r.from f where f.id = :id or r.to.id = :id", Route.class)
                .setParameter("id", id).getResultList();
    }

    private String filter() {
        return "where (:name = '' or r.name = :name) and (:fromName = '' or f.name = :fromName) "
                + "and (:toName = '' or t.name = :toName)";
    }

    private void parameters(Query query, String name, String fromName, String toName) {
        query.setParameter("name", name).setParameter("fromName", fromName).setParameter("toName", toName);
    }
}
