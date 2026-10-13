package ru.itmo.routes.api;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import ru.itmo.routes.dto.Requests.RouteInput;
import ru.itmo.routes.dto.Views.*;
import ru.itmo.routes.service.RouteService;

@Path("/routes")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RouteResource {
    @Inject private RouteService routes;

    @GET
    public Page<RouteView> list(@QueryParam("page") @DefaultValue("0") int page,
                                 @QueryParam("size") @DefaultValue("10") int size,
                                 @QueryParam("name") @DefaultValue("") String name,
                                 @QueryParam("fromName") @DefaultValue("") String fromName,
                                 @QueryParam("toName") @DefaultValue("") String toName,
                                 @QueryParam("sort") @DefaultValue("id") String sort,
                                 @QueryParam("direction") @DefaultValue("asc") String direction) {
        return routes.list(page, size, name, fromName, toName, sort, direction);
    }

    @GET @Path("/{id}")
    public RouteView get(@PathParam("id") @Positive long id) {
        return routes.get(id);
    }

    @POST
    public Response create(@Valid @NotNull RouteInput input, @Context UriInfo uri) {
        RouteView value = routes.create(input);
        return Response.created(uri.getAbsolutePathBuilder().path(value.id()).build()).entity(value).build();
    }

    @PUT @Path("/{id}")
    public RouteView update(@PathParam("id") @Positive long id, @Valid @NotNull RouteInput input) {
        return routes.update(id, input);
    }

    @DELETE @Path("/{id}")
    public Response delete(@PathParam("id") @Positive long id, @QueryParam("version") @NotNull @PositiveOrZero Long version) {
        routes.delete(id, version);
        return Response.noContent().build();
    }
}
