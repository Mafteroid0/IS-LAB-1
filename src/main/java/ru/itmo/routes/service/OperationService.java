package ru.itmo.routes.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ru.itmo.routes.dto.Requests.RouteInput;
import ru.itmo.routes.dto.Views.*;
import ru.itmo.routes.repository.*;
import ru.itmo.routes.error.Problem;
import java.util.*;
import static ru.itmo.routes.error.Problem.Kind.INVALID;

@ApplicationScoped
@Transactional
public class OperationService {
    @Inject private OperationRepository operations;
    @Inject private ReferenceRepository references;
    @Inject private WriteCoordinator changes;

    public Average average() {
        return new Average(operations.average());
    }

    public long countLess(float rating) {
        return operations.countLess(rating);
    }

    public List<RouteView> contains(String part) {
        return operations.contains(part).stream().map(RouteView::of).toList();
    }

    public List<RouteView> between(long fromId, long toId, String sort, String direction) {
        if (!Set.of("id", "name", "creationDate", "distance", "rating").contains(sort))
            throw new Problem(INVALID, "Сортировка: id, name, creationDate, distance или rating");
        if (!Set.of("asc", "desc").contains(direction))
            throw new Problem(INVALID, "Направление сортировки: asc или desc");
        requireLocation(fromId);
        requireLocation(toId);
        return operations.between(fromId, toId, sort, direction.equals("desc")).stream().map(RouteView::of).toList();
    }

    public RouteView addBetween(RouteInput input) {
        if (input.fromId == null) throw new Problem(INVALID, "Укажите локацию отправления");
        changes.changed();
        requireLocation(input.fromId);
        requireLocation(input.toId);
        Checks.found(references.coordinates(input.coordinatesId), "Координаты #" + input.coordinatesId);
        return RouteView.of(operations.addBetween(input));
    }

    private void requireLocation(long id) {
        Checks.found(references.location(id), "Локация #" + id);
    }
}
