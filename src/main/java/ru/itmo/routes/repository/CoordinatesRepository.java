package ru.itmo.routes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.routes.model.Coordinates;
import java.util.List;

public interface CoordinatesRepository extends JpaRepository<Coordinates, Long> {
    List<Coordinates> findAllByOrderByIdAsc();
}
