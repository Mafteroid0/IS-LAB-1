package ru.itmo.routes.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.itmo.routes.dto.Requests.LocationInput;
import ru.itmo.routes.dto.Views.Place;
import ru.itmo.routes.service.ReferenceService;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/api/locations", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class LocationController {
    private final ReferenceService references;

    public LocationController(ReferenceService references) {
        this.references = references;
    }

    @GetMapping
    public List<Place> all() {
        return references.locations();
    }

    @PostMapping
    public ResponseEntity<Place> create(@Valid @RequestBody LocationInput input) {
        Place value = references.createLocation(input);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(value.id()).toUri()).body(value);
    }

    @PutMapping("/{id}")
    public Place update(@PathVariable @Positive long id, @Valid @RequestBody LocationInput input) {
        return references.updateLocation(id, input);
    }

    @GetMapping("/{id}/usage")
    public Map<String, Long> usage(@PathVariable @Positive long id) {
        return Map.of("count", references.locationUsage(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Positive long id,
                                       @RequestParam(required = false) @NotNull @PositiveOrZero Long version,
                                       @RequestParam(required = false) @Positive Long replacementId) {
        references.deleteLocation(id, version, replacementId);
        return ResponseEntity.noContent().build();
    }
}
