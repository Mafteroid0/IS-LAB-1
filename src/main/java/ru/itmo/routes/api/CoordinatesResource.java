package ru.itmo.routes.api;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import ru.itmo.routes.dto.Requests.CoordinatesInput;
import ru.itmo.routes.dto.Views.Coordinate;
import ru.itmo.routes.service.ReferenceService;
import java.util.*;

@Path("/coordinates")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CoordinatesResource {
    @Inject private ReferenceService references;

    @GET
    public List<Coordinate> all() {
        return references.coordinates();
    }

    @POST
    public Response create(@Valid @NotNull CoordinatesInput input, @Context UriInfo uri) {
        Coordinate value = references.createCoordinates(input);
        return Response.created(uri.getAbsolutePathBuilder().path(value.id()).build()).entity(value).build();
    }

    @PUT @Path("/{id}")
    public Coordinate update(@PathParam("id") @Positive long id, @Valid @NotNull CoordinatesInput input) {
        return references.updateCoordinates(id, input);
    }

    @GET @Path("/{id}/usage")
    public Map<String, Long> usage(@PathParam("id") @Positive long id) {
        return Map.of("count", references.coordinatesUsage(id));
    }

    @DELETE @Path("/{id}")
    public Response delete(@PathParam("id") @Positive long id,
                           @QueryParam("version") @NotNull @PositiveOrZero Long version,
                           @QueryParam("replacementId") @Positive Long replacementId) {
        references.deleteCoordinates(id, version, replacementId);
        return Response.noContent().build();
    }
}
