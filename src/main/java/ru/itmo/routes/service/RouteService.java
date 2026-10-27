package ru.itmo.routes.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ru.itmo.routes.dto.Requests.RouteInput;
import ru.itmo.routes.dto.Views.*;
import ru.itmo.routes.model.*;
import ru.itmo.routes.repository.*;
import ru.itmo.routes.error.*;
import java.util.Set;

@ApplicationScoped
@TranslateServiceErrors
@Transactional
public class RouteService {
    @Inject private RouteRepository routes;
    @Inject private ReferenceRepository references;
    @Inject private WriteCoordinator changes;

    public Page<RouteView> list(int page, int size, String name, String fromName, String toName,
                                String sort, String direction) {
        if (page < 0 || page > Integer.MAX_VALUE / 100 || size < 1 || size > 100)
            throw new InvalidInputException("Страница должна быть неотрицательной, размер — от 1 до 100");
        if (!Set.of("id", "name", "fromName", "toName", "creationDate", "distance", "rating").contains(sort))
            throw new InvalidInputException("Недопустимый параметр сортировки");
        if (!Set.of("asc", "desc").contains(direction))
            throw new InvalidInputException("Направление сортировки: asc или desc");
        long total = routes.count(name, fromName, toName);
        int selected = (int) Math.min(page, Math.max(0, (total - 1) / size));
        return new Page<>(routes.list(name, fromName, toName, sort, direction.equals("desc"), selected * size, size)
                .stream().map(RouteView::of).toList(), total, selected, size);
    }

    public RouteView get(long id) {
        return RouteView.of(require(id));
    }

    public RouteView create(RouteInput input) {
        changes.changed();
        Route value = new Route();
        apply(value, input);
        routes.save(value);
        routes.flush();
        return RouteView.of(value);
    }

    public RouteView update(long id, RouteInput input) {
        changes.changed();
        Route value = require(id);
        Checks.version(value.getVersion(), input.version);
        apply(value, input);
        routes.flush();
        return RouteView.of(value);
    }

    public void delete(long id, Long version) {
        changes.changed();
        Route value = require(id);
        Checks.version(value.getVersion(), version);
        routes.remove(value);
        routes.flush();
    }

    private Route require(long id) {
        return Checks.found(routes.find(id), "Маршрут #" + id);
    }

    private void apply(Route value, RouteInput input) {
        Coordinates point = Checks.found(references.coordinates(input.coordinatesId), "Координаты #" + input.coordinatesId);
        Location from = input.fromId == null ? null : Checks.found(references.location(input.fromId), "Локация #" + input.fromId);
        Location to = Checks.found(references.location(input.toId), "Локация #" + input.toId);
        value.change(input.name, point, from, to, input.distance, input.rating);
    }
}
