package ru.itmo.routes.api;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import ru.itmo.routes.service.WriteCoordinator;
import java.util.Map;

@Path("/state")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class StateResource {
    @Inject private WriteCoordinator changes;

    @GET
    public Map<String, String> state() {
        return Map.of("revision", changes.revision());
    }
}
