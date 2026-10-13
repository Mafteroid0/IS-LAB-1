package ru.itmo.routes.api;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ValidationErrorMapper implements ExceptionMapper<ConstraintViolationException> {
    public Response toResponse(ConstraintViolationException error) {
        return new ErrorMapper().toResponse(error);
    }
}
