package ru.itmo.routes.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import ru.itmo.routes.dto.Requests.*;
import ru.itmo.routes.dto.Views.*;
import ru.itmo.routes.model.*;
import ru.itmo.routes.repository.*;
import ru.itmo.routes.error.*;
import java.util.List;

@ApplicationScoped
@TranslateServiceErrors
@Transactional
public class ReferenceService {
    @Inject private ReferenceRepository references;
    @Inject private RouteRepository routes;
    @Inject private WriteCoordinator changes;

    public List<Coordinate> coordinates() {
        return references.coordinates().stream().map(Coordinate::of).toList();
    }

    public List<Place> locations() {
        return references.locations().stream().map(Place::of).toList();
    }

    public Coordinate createCoordinates(CoordinatesInput input) {
        changes.changed();
        Coordinates value = new Coordinates(input.x, input.y);
        references.save(value);
        references.flush();
        return Coordinate.of(value);
    }

    public Coordinate updateCoordinates(long id, CoordinatesInput input) {
        changes.changed();
        Coordinates value = requireCoordinates(id);
        Checks.version(value.getVersion(), input.version);
        value.change(input.x, input.y);
        references.flush();
        return Coordinate.of(value);
    }

    public Place createLocation(LocationInput input) {
        changes.changed();
        Location value = new Location(input.x, input.y, input.z, input.name);
        references.save(value);
        references.flush();
        return Place.of(value);
    }

    public Place updateLocation(long id, LocationInput input) {
        changes.changed();
        Location value = requireLocation(id);
        Checks.version(value.getVersion(), input.version);
        value.change(input.x, input.y, input.z, input.name);
        references.flush();
        return Place.of(value);
    }

    public long coordinatesUsage(long id) {
        requireCoordinates(id);
        return routes.byCoordinates(id).size();
    }

    public long locationUsage(long id) {
        requireLocation(id);
        return routes.byLocation(id).size();
    }

    public void deleteCoordinates(long id, Long version, Long replacementId) {
        changes.changed();
        Coordinates value = requireCoordinates(id);
        Checks.version(value.getVersion(), version);
        checkReplacement(id, replacementId);
        Coordinates replacement = replacementId == null ? null : requireCoordinates(replacementId);
        List<Route> linked = routes.byCoordinates(id);
        requireReplacement(linked, replacement);
        linked.forEach(route -> route.moveCoordinates(replacement));
        routes.flush();
        references.remove(value);
        references.flush();
    }

    public void deleteLocation(long id, Long version, Long replacementId) {
        changes.changed();
        Location value = requireLocation(id);
        Checks.version(value.getVersion(), version);
        checkReplacement(id, replacementId);
        Location replacement = replacementId == null ? null : requireLocation(replacementId);
        List<Route> linked = routes.byLocation(id);
        requireReplacement(linked, replacement);
        linked.forEach(route -> route.replaceLocation(id, replacement));
        routes.flush();
        references.remove(value);
        references.flush();
    }

    private Coordinates requireCoordinates(long id) {
        return Checks.found(references.coordinates(id), "Координаты #" + id);
    }

    private Location requireLocation(long id) {
        return Checks.found(references.location(id), "Локация #" + id);
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
