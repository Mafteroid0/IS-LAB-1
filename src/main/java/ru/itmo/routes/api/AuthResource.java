package ru.itmo.routes.api;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import ru.itmo.routes.dto.Requests.Credentials;
import ru.itmo.routes.service.AuthService;
import java.util.Map;

@Path("/auth")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {
    @Inject private AuthService auth;
    @Context private HttpServletRequest request;

    @POST @Path("/register")
    public Response register(@Valid @NotNull Credentials input) {
        return Response.status(201).entity(signIn(auth.register(input))).build();
    }

    @POST @Path("/login")
    public Map<String, String> login(@Valid @NotNull Credentials input) {
        return signIn(auth.login(input));
    }

    @GET @Path("/me")
    public Map<String, String> me() {
        return Map.of("username", (String) request.getSession(false).getAttribute("username"));
    }

    @POST @Path("/logout")
    public Response logout() {
        request.getSession(false).invalidate();
        return Response.noContent().build();
    }

    private Map<String, String> signIn(String username) {
        HttpSession old = request.getSession(false);
        if (old != null) old.invalidate();
        request.getSession(true).setAttribute("username", username);
        return Map.of("username", username);
    }
}
