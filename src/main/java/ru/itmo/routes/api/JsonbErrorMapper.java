package ru.itmo.routes.api;

import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class JsonbErrorMapper implements ExceptionMapper<JsonbException> {
    @Override
    public Response toResponse(JsonbException error) {
        return ErrorResponses.create(400, "Некорректный JSON: проверьте синтаксис, типы и диапазоны чисел");
    }
}
