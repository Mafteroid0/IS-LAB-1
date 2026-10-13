package ru.itmo.routes.api;

import jakarta.annotation.Priority;
import jakarta.servlet.http.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.*;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.Provider;
import java.util.Set;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class SecurityFilter implements ContainerRequestFilter, ContainerResponseFilter {
    @Context private HttpServletRequest request;

    public void filter(ContainerRequestContext context) {
        String path = context.getUriInfo().getPath();
        boolean publicEndpoint = Set.of("auth/login", "auth/register").contains(path)
                && context.getMethod().equals("POST");
        HttpSession session = request.getSession(false);
        if (!publicEndpoint && (session == null || session.getAttribute("username") == null))
            throw new ClientErrorException("Войдите в систему", 401);
        if (!Set.of("GET", "HEAD", "OPTIONS").contains(context.getMethod())
                && !"RouteLab".equals(context.getHeaderString("X-Requested-With")))
            throw new ForbiddenException("Запрос не прошёл проверку: отсутствует X-Requested-With");
    }

    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        response.getHeaders().putSingle("Cache-Control", "no-store");
        response.getHeaders().putSingle("X-Content-Type-Options", "nosniff");
    }
}
