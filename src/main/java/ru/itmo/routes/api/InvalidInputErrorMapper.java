package ru.itmo.routes.api;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ru.itmo.routes.error.InvalidInputException;

@Provider
public class InvalidInputErrorMapper implements ExceptionMapper<InvalidInputException> {
    @Override
    public Response toResponse(InvalidInputException error) {
        return ErrorResponses.create(400, error.getMessage());
    }
}
