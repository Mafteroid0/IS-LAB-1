package ru.itmo.routes.api;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ru.itmo.routes.error.DatabaseException;

@Provider
public class DatabaseErrorMapper implements ExceptionMapper<DatabaseException> {
    @Override
    public Response toResponse(DatabaseException error) {
        ErrorResponses.log(error);
        return ErrorResponses.create(500, error.getMessage());
    }
}
