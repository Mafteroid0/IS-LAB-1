package ru.itmo.routes.api;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ru.itmo.routes.error.ConflictException;

@Provider
public class ConflictErrorMapper implements ExceptionMapper<ConflictException> {
    @Override
    public Response toResponse(ConflictException error) {
        return ErrorResponses.create(409, error.getMessage());
    }
}
