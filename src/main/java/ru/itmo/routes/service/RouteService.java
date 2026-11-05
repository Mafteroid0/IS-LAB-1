package ru.itmo.routes.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.routes.dto.Requests.RouteInput;
import ru.itmo.routes.dto.Views.*;
import ru.itmo.routes.error.InvalidInputException;
import ru.itmo.routes.model.*;
import ru.itmo.routes.repository.*;
import java.util.Set;

@Service
@Transactional
public class RouteService {
    private final RouteRepository routes;
    private final CoordinatesRepository coordinates;
    private final LocationRepository locations;
    private final WriteCoordinator changes;

    public RouteService(RouteRepository routes, CoordinatesRepository coordinates,
                        LocationRepository locations, WriteCoordinator changes) {
        this.routes = routes;
        this.coordinates = coordinates;
        this.locations = locations;
        this.changes = changes;
    }

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
        routes.saveAndFlush(value);
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
        routes.delete(value);
        routes.flush();
    }

    private Route require(long id) {
        return Checks.found(routes.findById(id), "Маршрут #" + id);
    }

    private void apply(Route value, RouteInput input) {
        Coordinates point = Checks.found(coordinates.findById(input.coordinatesId), "Координаты #" + input.coordinatesId);
        Location from = input.fromId == null ? null : Checks.found(locations.findById(input.fromId), "Локация #" + input.fromId);
        Location to = Checks.found(locations.findById(input.toId), "Локация #" + input.toId);
        value.change(input.name, point, from, to, input.distance, input.rating);
    }
}
