package ru.itmo.routes.api;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import ru.itmo.routes.dto.Requests.RouteInput;
import ru.itmo.routes.dto.Views.*;
import ru.itmo.routes.service.OperationService;
import ru.itmo.routes.validation.Finite;
import java.util.*;

@Path("/operations")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OperationResource {
    @Inject private OperationService operations;

    @GET @Path("/average-rating")
    public Average average() {
        return operations.average();
    }

    @GET @Path("/rating-less")
    public Map<String, Long> countLess(@QueryParam("rating") @NotNull @Finite Float rating) {
        return Map.of("count", operations.countLess(rating));
    }

    @GET @Path("/name-contains")
    public List<RouteView> contains(@QueryParam("part") @DefaultValue("") String part) {
        return operations.contains(part);
    }

    @GET @Path("/between")
    public List<RouteView> between(@QueryParam("fromId") @NotNull @Positive Long fromId,
                                  @QueryParam("toId") @NotNull @Positive Long toId,
                                  @QueryParam("sort") @DefaultValue("id") String sort,
                                  @QueryParam("direction") @DefaultValue("asc") String direction) {
        return operations.between(fromId, toId, sort, direction);
    }

    @POST @Path("/between")
    public Response addBetween(@Valid @NotNull RouteInput input) {
        return Response.status(201).entity(operations.addBetween(input)).build();
    }
}
