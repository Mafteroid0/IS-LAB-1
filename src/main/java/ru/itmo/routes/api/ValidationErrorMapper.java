package ru.itmo.routes.api;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class ValidationErrorMapper implements ExceptionMapper<ConstraintViolationException> {
    @Override
    public Response toResponse(ConstraintViolationException error) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (var violation : error.getConstraintViolations()) {
            for (Path.Node node : violation.getPropertyPath()) {
                if (node.getKind() == ElementKind.RETURN_VALUE) {
                    ErrorResponses.log(error);
                    return ErrorResponses.create(500, ErrorResponses.SERVER_ERROR);
                }
            }
            String path = violation.getPropertyPath().toString();
            fields.putIfAbsent(path.substring(path.lastIndexOf('.') + 1), violation.getMessage());
        }
        return ErrorResponses.create(400, "Исправьте значения полей", fields);
    }
}
