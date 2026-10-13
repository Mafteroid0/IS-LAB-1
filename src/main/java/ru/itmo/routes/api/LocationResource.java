package ru.itmo.routes.api;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import ru.itmo.routes.dto.Requests.LocationInput;
import ru.itmo.routes.dto.Views.Place;
import ru.itmo.routes.service.ReferenceService;
import java.util.*;

@Path("/locations")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LocationResource {
    @Inject private ReferenceService references;

    @GET
    public List<Place> all() {
        return references.locations();
    }

    @POST
    public Response create(@Valid @NotNull LocationInput input, @Context UriInfo uri) {
        Place value = references.createLocation(input);
        return Response.created(uri.getAbsolutePathBuilder().path(value.id()).build()).entity(value).build();
    }

    @PUT @Path("/{id}")
    public Place update(@PathParam("id") @Positive long id, @Valid @NotNull LocationInput input) {
        return references.updateLocation(id, input);
    }

    @GET @Path("/{id}/usage")
    public Map<String, Long> usage(@PathParam("id") @Positive long id) {
        return Map.of("count", references.locationUsage(id));
    }

    @DELETE @Path("/{id}")
    public Response delete(@PathParam("id") @Positive long id,
                           @QueryParam("version") @NotNull @PositiveOrZero Long version,
                           @QueryParam("replacementId") @Positive Long replacementId) {
        references.deleteLocation(id, version, replacementId);
        return Response.noContent().build();
    }
}
