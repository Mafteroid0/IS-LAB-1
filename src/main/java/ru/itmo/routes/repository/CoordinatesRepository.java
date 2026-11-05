package ru.itmo.routes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.routes.model.Coordinates;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CoordinatesRepository extends JpaRepository<Coordinates, Long> {
    List<Coordinates> findAllByOrderByIdAsc();

    @Query("select c from Coordinates c where c.x = :x and c.y = :y and c.id <> :excluded order by c.id")
    List<Coordinates> findSame(@Param("x") float x, @Param("y") float y, @Param("excluded") long excluded);
}
