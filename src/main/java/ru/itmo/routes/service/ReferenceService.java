package ru.itmo.routes.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.routes.dto.Requests.*;
import ru.itmo.routes.dto.Views.*;
import ru.itmo.routes.error.*;
import ru.itmo.routes.model.*;
import ru.itmo.routes.repository.*;
import java.util.List;

@Service
@Transactional
public class ReferenceService {
    private final CoordinatesRepository coordinates;
    private final LocationRepository locations;
    private final RouteRepository routes;
    private final WriteCoordinator changes;

    public ReferenceService(CoordinatesRepository coordinates, LocationRepository locations,
                            RouteRepository routes, WriteCoordinator changes) {
        this.coordinates = coordinates;
        this.locations = locations;
        this.routes = routes;
        this.changes = changes;
    }

    public List<Coordinate> coordinates() {
        return coordinates.findAllByOrderByIdAsc().stream().map(Coordinate::of).toList();
    }

    public List<Place> locations() {
        return locations.findAllByOrderByNameAscIdAsc().stream().map(Place::of).toList();
    }

    public Coordinate createCoordinates(CoordinatesInput input) {
        changes.changed();
        return Coordinate.of(coordinates.saveAndFlush(new Coordinates(input.x, input.y)));
    }

    public Coordinate updateCoordinates(long id, CoordinatesInput input) {
        changes.changed();
        Coordinates value = requireCoordinates(id);
        Checks.version(value.getVersion(), input.version);
        value.change(input.x, input.y);
        coordinates.flush();
        return Coordinate.of(value);
    }

    public Place createLocation(LocationInput input) {
        changes.changed();
        return Place.of(locations.saveAndFlush(new Location(input.x, input.y, input.z, input.name)));
    }

    public Place updateLocation(long id, LocationInput input) {
        changes.changed();
        Location value = requireLocation(id);
        Checks.version(value.getVersion(), input.version);
        value.change(input.x, input.y, input.z, input.name);
        locations.flush();
        return Place.of(value);
    }

    public long coordinatesUsage(long id) {
        requireCoordinates(id);
        return routes.findByCoordinates(id).size();
    }

    public long locationUsage(long id) {
        requireLocation(id);
        return routes.findByLocation(id).size();
    }

    public void deleteCoordinates(long id, Long version, Long replacementId) {
        changes.changed();
        Coordinates value = requireCoordinates(id);
        Checks.version(value.getVersion(), version);
        checkReplacement(id, replacementId);
        Coordinates replacement = replacementId == null ? null : requireCoordinates(replacementId);
        List<Route> linked = routes.findByCoordinates(id);
        requireReplacement(linked, replacement);
        linked.forEach(route -> route.moveCoordinates(replacement));
        routes.flush();
        coordinates.delete(value);
        coordinates.flush();
    }

    public void deleteLocation(long id, Long version, Long replacementId) {
        changes.changed();
        Location value = requireLocation(id);
        Checks.version(value.getVersion(), version);
        checkReplacement(id, replacementId);
        Location replacement = replacementId == null ? null : requireLocation(replacementId);
        List<Route> linked = routes.findByLocation(id);
        requireReplacement(linked, replacement);
        linked.forEach(route -> route.replaceLocation(id, replacement));
        routes.flush();
        locations.delete(value);
        locations.flush();
    }

    private Coordinates requireCoordinates(long id) {
        return Checks.found(coordinates.findById(id), "Координаты #" + id);
    }

    private Location requireLocation(long id) {
        return Checks.found(locations.findById(id), "Локация #" + id);
    }

    private void checkReplacement(long id, Long replacementId) {
        if (replacementId != null && replacementId == id)
            throw new InvalidInputException("Для замены выберите другой объект");
    }

    private void requireReplacement(List<Route> linked, Object replacement) {
        if (!linked.isEmpty() && replacement == null)
            throw new ConflictException("Объект используется маршрутами. Выберите замену.");
    }
}
