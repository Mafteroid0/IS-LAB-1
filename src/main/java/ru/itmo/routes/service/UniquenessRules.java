package ru.itmo.routes.service;

import org.springframework.stereotype.Component;
import ru.itmo.routes.error.ConflictException;
import ru.itmo.routes.model.Coordinates;
import ru.itmo.routes.model.Location;
import ru.itmo.routes.repository.CoordinatesRepository;
import ru.itmo.routes.repository.LocationRepository;
import ru.itmo.routes.repository.RouteRepository;
import java.util.List;
import java.util.stream.Stream;

/**
 * Ограничения уникальности предметной области. Они проверяются только здесь, на уровне бизнес-логики,
 * и намеренно не продублированы в базе данных. Методы вызываются после WriteCoordinator.changed(), то есть
 * под глобальной блокировкой записи: проверка и вставка не могут чередоваться с другой записью.
 * <ul>
 *   <li>название маршрута уникально без учёта регистра и крайних пробелов;</li>
 *   <li>название локации уникально по тому же правилу;</li>
 *   <li>пара координат (x, y) уникальна.</li>
 * </ul>
 * Параметр excluded — id редактируемого объекта (0 для нового, так как реальные id положительны).
 */
@Component
public class UniquenessRules {
    private final RouteRepository routes;
    private final LocationRepository locations;
    private final CoordinatesRepository coordinates;

    public UniquenessRules(RouteRepository routes, LocationRepository locations, CoordinatesRepository coordinates) {
        this.routes = routes;
        this.locations = locations;
        this.coordinates = coordinates;
    }

    public void routeName(String name, long excluded) {
        String key = name.strip();
        if (routes.namesExcept(excluded).stream().anyMatch(value -> key.equalsIgnoreCase(value.strip())))
            throw new ConflictException("Маршрут с названием «" + name.strip() + "» уже существует");
    }

    public void locationName(String name, long excluded) {
        if (locationMatches(name, excluded).findAny().isPresent())
            throw new ConflictException("Локация с названием «" + name.strip() + "» уже существует");
    }

    public void coordinates(float x, float y, long excluded) {
        if (!coordinates.findSame(x, y, excluded).isEmpty())
            throw new ConflictException("Координаты (" + x + "; " + y + ") уже существуют");
    }

    /** Существующие локации с тем же названием (для импорта вложенных объектов). */
    public List<Location> sameLocations(String name) {
        return locationMatches(name, 0).toList();
    }

    private Stream<Location> locationMatches(String name, long excluded) {
        String key = name.strip();
        return locations.findForUniqueness(excluded).stream()
                .filter(location -> key.equalsIgnoreCase(location.getName().strip()));
    }

    public List<Coordinates> sameCoordinates(float x, float y) {
        return coordinates.findSame(x, y, 0);
    }
}
