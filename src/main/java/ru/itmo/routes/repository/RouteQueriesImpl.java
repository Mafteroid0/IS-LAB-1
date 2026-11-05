package ru.itmo.routes.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import ru.itmo.routes.model.Route;
import java.util.List;

class RouteQueriesImpl implements RouteQueries {
    @PersistenceContext
    private EntityManager em;

    @Override
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

    @Override
    public long count(String name, String fromName, String toName) {
        TypedQuery<Long> query = em.createQuery("select count(r) from Route r left join r.from f join r.to t "
                + filter(), Long.class);
        parameters(query, name, fromName, toName);
        return query.getSingleResult();
    }

    private String filter() {
        return "where (:name = '' or r.name = :name) and (:fromName = '' or f.name = :fromName) "
                + "and (:toName = '' or t.name = :toName)";
    }

    private void parameters(Query query, String name, String fromName, String toName) {
        query.setParameter("name", name).setParameter("fromName", fromName).setParameter("toName", toName);
    }
}
