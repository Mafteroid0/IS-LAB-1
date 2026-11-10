package ru.itmo.routes.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.routes.dto.Requests.RouteInput;
import ru.itmo.routes.dto.Views.*;
import ru.itmo.routes.error.InvalidInputException;
import ru.itmo.routes.repository.*;
import org.springframework.transaction.annotation.Isolation;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class OperationService {
    private final OperationRepository operations;
    private final CoordinatesRepository coordinates;
    private final LocationRepository locations;
    private final WriteCoordinator changes;
    private final UniquenessRules rules;

    public OperationService(OperationRepository operations, CoordinatesRepository coordinates,
                            LocationRepository locations, WriteCoordinator changes, UniquenessRules rules) {
        this.rules = rules;
        this.operations = operations;
        this.coordinates = coordinates;
        this.locations = locations;
        this.changes = changes;
    }

    public Average average() {
        return new Average(operations.average());
    }

    public long countLess(float rating) {
        return operations.countLess(rating);
    }

    public List<RouteView> contains(String part) {
        return operations.contains(part).stream().map(RouteView::of).toList();
    }

    /** Проверка локаций и выборка — разные запросы; один снимок исключает «исчезнувшую» между ними локацию. */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<RouteView> between(long fromId, long toId, String sort, String direction) {
        if (!Set.of("id", "name", "creationDate", "distance", "rating").contains(sort))
            throw new InvalidInputException("Сортировка: id, name, creationDate, distance или rating");
        if (!Set.of("asc", "desc").contains(direction))
            throw new InvalidInputException("Направление сортировки: asc или desc");
        requireLocation(fromId);
        requireLocation(toId);
        return operations.between(fromId, toId, sort, direction.equals("desc")).stream().map(RouteView::of).toList();
    }

    public RouteView addBetween(RouteInput input) {
        if (input.fromId == null) throw new InvalidInputException("Укажите локацию отправления");
        changes.changed();
        rules.routeName(input.name, 0);
        requireLocation(input.fromId);
        requireLocation(input.toId);
        Checks.found(coordinates.findById(input.coordinatesId), "Координаты #" + input.coordinatesId);
        return RouteView.of(operations.addBetween(input));
    }

    private void requireLocation(long id) {
        Checks.found(locations.findById(id), "Локация #" + id);
    }
}
