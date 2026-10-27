package ru.itmo.routes.api;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ru.itmo.routes.error.DatabaseUnavailableException;

@Provider
public class DatabaseUnavailableErrorMapper implements ExceptionMapper<DatabaseUnavailableException> {
    @Override
    public Response toResponse(DatabaseUnavailableException error) {
        ErrorResponses.log(error);
        return ErrorResponses.create(503, error.getMessage());
    }
}
