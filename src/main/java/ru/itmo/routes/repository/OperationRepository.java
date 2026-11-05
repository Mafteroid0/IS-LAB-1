package ru.itmo.routes.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import ru.itmo.routes.dto.Requests.RouteInput;
import ru.itmo.routes.model.Route;
import java.util.List;

/** Вызовы SQL-функций специальных операций (см. миграцию V2). */
@Repository
public class OperationRepository {
    @PersistenceContext
    private EntityManager em;

    public Double average() {
        Number value = (Number) em.createNativeQuery("select rl_average_rating()").getSingleResult();
        return value == null ? null : value.doubleValue();
    }

    public long countLess(float rating) {
        return ((Number) em.createNativeQuery("select rl_count_rating_less(cast(:rating as real))")
                .setParameter("rating", rating).getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    public List<Route> contains(String part) {
        return em.createNativeQuery("select * from rl_routes_name_contains(cast(:part as text))", Route.class)
                .setParameter("part", part).getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Route> between(long fromId, long toId, String sort, boolean descending) {
        return em.createNativeQuery("select * from rl_routes_between(cast(:fromId as bigint), cast(:toId as bigint), "
                + "cast(:sort as text), cast(:descending as boolean))", Route.class)
                .setParameter("fromId", fromId).setParameter("toId", toId)
                .setParameter("sort", sort).setParameter("descending", descending).getResultList();
    }

    public Route addBetween(RouteInput input) {
        em.flush();
        return (Route) em.createNativeQuery(
                "select * from rl_add_between(cast(:name as text), cast(:coordinatesId as bigint), "
                + "cast(:fromId as bigint), cast(:toId as bigint), cast(:distance as real), cast(:rating as real))", Route.class)
                .setParameter("name", input.name).setParameter("coordinatesId", input.coordinatesId)
                .setParameter("fromId", input.fromId).setParameter("toId", input.toId)
                .setParameter("distance", input.distance).setParameter("rating", input.rating).getSingleResult();
    }
}
