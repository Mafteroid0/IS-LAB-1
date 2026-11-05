package ru.itmo.routes.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.itmo.routes.dto.Requests.RouteInput;
import ru.itmo.routes.dto.Views.*;
import ru.itmo.routes.service.OperationService;
import ru.itmo.routes.validation.Finite;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/api/operations", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
public class OperationController {
    private final OperationService operations;

    public OperationController(OperationService operations) {
        this.operations = operations;
    }

    @GetMapping("/average-rating")
    public Average average() {
        return operations.average();
    }

    @GetMapping("/rating-less")
    public Map<String, Long> countLess(@RequestParam(required = false) @NotNull @Finite Float rating) {
        return Map.of("count", operations.countLess(rating));
    }

    @GetMapping("/name-contains")
    public List<RouteView> contains(@RequestParam(defaultValue = "") String part) {
        return operations.contains(part);
    }

    @GetMapping("/between")
    public List<RouteView> between(@RequestParam(required = false) @NotNull @Positive Long fromId,
                                   @RequestParam(required = false) @NotNull @Positive Long toId,
                                   @RequestParam(defaultValue = "id") String sort,
                                   @RequestParam(defaultValue = "asc") String direction) {
        return operations.between(fromId, toId, sort, direction);
    }

    @PostMapping("/between")
    public ResponseEntity<RouteView> addBetween(@Valid @RequestBody RouteInput input) {
        return ResponseEntity.status(201).body(operations.addBetween(input));
    }
}
