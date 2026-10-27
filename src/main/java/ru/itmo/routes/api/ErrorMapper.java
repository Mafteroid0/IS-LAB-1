package ru.itmo.routes.api;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ErrorMapper implements ExceptionMapper<Exception> {
    @Override
    public Response toResponse(Exception error) {
        ErrorResponses.log(error);
        return ErrorResponses.create(500, ErrorResponses.SERVER_ERROR);
    }
}
