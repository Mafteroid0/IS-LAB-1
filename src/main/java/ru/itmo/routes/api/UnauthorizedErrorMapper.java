package ru.itmo.routes.api;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ru.itmo.routes.error.UnauthorizedException;

@Provider
public class UnauthorizedErrorMapper implements ExceptionMapper<UnauthorizedException> {
    @Override
    public Response toResponse(UnauthorizedException error) {
        return ErrorResponses.create(401, error.getMessage());
    }
}
