package ru.itmo.routes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.routes.model.Location;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface LocationRepository extends JpaRepository<Location, Long> {
    List<Location> findAllByOrderByNameAscIdAsc();

    @Query("select l from Location l where l.id <> :excluded order by l.id")
    List<Location> findForUniqueness(@Param("excluded") long excluded);
}
