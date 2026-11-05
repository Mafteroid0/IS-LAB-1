package ru.itmo.routes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.itmo.routes.model.Route;
import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long>, RouteQueries {
    @Query("select r from Route r where r.coordinates.id = :id")
    List<Route> findByCoordinates(@Param("id") long id);

    @Query("select r from Route r left join r.from f where f.id = :id or r.to.id = :id")
    List<Route> findByLocation(@Param("id") long id);
}
