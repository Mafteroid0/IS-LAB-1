package ru.itmo.routes.api;

import jakarta.json.stream.JsonParsingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class JsonParsingErrorMapper implements ExceptionMapper<JsonParsingException> {
    @Override
    public Response toResponse(JsonParsingException error) {
        return ErrorResponses.create(400, "Некорректный JSON: проверьте синтаксис, типы и диапазоны чисел");
    }
}
