package ru.itmo.routes.api;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ru.itmo.routes.error.ResourceNotFoundException;

@Provider
public class ResourceNotFoundErrorMapper implements ExceptionMapper<ResourceNotFoundException> {
    @Override
    public Response toResponse(ResourceNotFoundException error) {
        return ErrorResponses.create(404, error.getMessage());
    }
}
