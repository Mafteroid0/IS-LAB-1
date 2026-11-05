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
import ru.itmo.routes.dto.Requests.CoordinatesInput;
import ru.itmo.routes.dto.Views.Coordinate;
import ru.itmo.routes.service.ReferenceService;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/api/coordinates", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class CoordinatesController {
    private final ReferenceService references;

    public CoordinatesController(ReferenceService references) {
        this.references = references;
    }

    @GetMapping
    public List<Coordinate> all() {
        return references.coordinates();
    }

    @PostMapping
    public ResponseEntity<Coordinate> create(@Valid @RequestBody CoordinatesInput input) {
        Coordinate value = references.createCoordinates(input);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(value.id()).toUri()).body(value);
    }

    @PutMapping("/{id}")
    public Coordinate update(@PathVariable @Positive long id, @Valid @RequestBody CoordinatesInput input) {
        return references.updateCoordinates(id, input);
    }

    @GetMapping("/{id}/usage")
    public Map<String, Long> usage(@PathVariable @Positive long id) {
        return Map.of("count", references.coordinatesUsage(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Positive long id,
                                       @RequestParam(required = false) @NotNull @PositiveOrZero Long version,
                                       @RequestParam(required = false) @Positive Long replacementId) {
        references.deleteCoordinates(id, version, replacementId);
        return ResponseEntity.noContent().build();
    }
}
