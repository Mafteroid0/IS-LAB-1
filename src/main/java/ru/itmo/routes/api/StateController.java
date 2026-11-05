package ru.itmo.routes.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.routes.service.WriteCoordinator;
import java.util.Map;

/** Текущая ревизия данных: клиент опрашивает её, чтобы обновлять таблицу после чужих изменений. */
@RestController
@RequestMapping(path = "/api/state", produces = MediaType.APPLICATION_JSON_VALUE)
public class StateController {
    private final WriteCoordinator changes;

    public StateController(WriteCoordinator changes) {
        this.changes = changes;
    }

    @GetMapping
    public Map<String, String> state() {
        return Map.of("revision", changes.revision());
    }
}
