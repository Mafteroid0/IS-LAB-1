package ru.itmo.routes.api;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

@Provider
public class HttpErrorMapper implements ExceptionMapper<WebApplicationException> {
    @Override
    public Response toResponse(WebApplicationException error) {
        int status = error.getResponse().getStatus();
        String message = switch (status) {
            case 400 -> "Некорректный запрос: проверьте JSON, параметры, типы и диапазоны чисел";
            case 404 -> "Ресурс не найден";
            case 405 -> "Метод запроса не поддерживается";
            case 415 -> "Неподдерживаемый формат данных. Используйте application/json";
            default -> error.getMessage();
        };
        if (status >= 500) {
            ErrorResponses.log(error);
            message = ErrorResponses.SERVER_ERROR;
        }
        return Response.fromResponse(error.getResponse()).type(MediaType.APPLICATION_JSON)
                .header("Content-Length", null)
                .entity(ErrorResponses.body(message, Map.of())).build();
    }
}
