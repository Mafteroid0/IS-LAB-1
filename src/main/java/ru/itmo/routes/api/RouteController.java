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
import ru.itmo.routes.dto.Requests.RouteInput;
import ru.itmo.routes.dto.Views.*;
import ru.itmo.routes.service.RouteService;

@RestController
@RequestMapping(path = "/api/routes", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class RouteController {
    private final RouteService routes;

    public RouteController(RouteService routes) {
        this.routes = routes;
    }

    @GetMapping
    public Page<RouteView> list(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                @RequestParam(defaultValue = "") String name,
                                @RequestParam(defaultValue = "") String fromName,
                                @RequestParam(defaultValue = "") String toName,
                                @RequestParam(defaultValue = "id") String sort,
                                @RequestParam(defaultValue = "asc") String direction) {
        return routes.list(page, size, name, fromName, toName, sort, direction);
    }

    @GetMapping("/{id}")
    public RouteView get(@PathVariable @Positive long id) {
        return routes.get(id);
    }

    @PostMapping
    public ResponseEntity<RouteView> create(@Valid @RequestBody RouteInput input) {
        RouteView value = routes.create(input);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(value.id()).toUri()).body(value);
    }

    @PutMapping("/{id}")
    public RouteView update(@PathVariable @Positive long id, @Valid @RequestBody RouteInput input) {
        return routes.update(id, input);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @Positive long id,
                                       @RequestParam(required = false) @NotNull @PositiveOrZero Long version) {
        routes.delete(id, version);
        return ResponseEntity.noContent().build();
    }
}
