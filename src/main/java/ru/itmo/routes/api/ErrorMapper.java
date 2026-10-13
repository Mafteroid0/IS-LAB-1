package ru.itmo.routes.api;

import jakarta.persistence.*;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.*;
import java.util.*;
import java.util.logging.*;
import java.sql.SQLException;
import ru.itmo.routes.error.Problem;

@Provider
public class ErrorMapper implements ExceptionMapper<Exception> {
    private static final Logger LOG = Logger.getLogger(ErrorMapper.class.getName());

    public Response toResponse(Exception error) {
        int status = 500;
        String message = "Ошибка сервера. Повторите запрос позже.";
        Map<String, String> fields = new LinkedHashMap<>();
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException invalid) {
                invalid.getConstraintViolations().forEach(v -> {
                    String path = v.getPropertyPath().toString();
                    fields.put(path.substring(path.lastIndexOf('.') + 1), v.getMessage());
                });
                status = 400;
                message = "Исправьте значения полей";
                break;
            }
            if (cause instanceof WebApplicationException http) {
                status = http.getResponse().getStatus();
                message = http.getMessage();
                if (status == 400) message = "Некорректный запрос: " + message;
            }
            if (cause instanceof Problem problem) {
                status = switch (problem.getKind()) {
                    case INVALID -> 400;
                    case NOT_FOUND -> 404;
                    case CONFLICT -> 409;
                    case UNAUTHORIZED -> 401;
                };
                message = problem.getMessage();
            }
            if (cause instanceof OptimisticLockException) {
                status = 409;
                message = "Объект уже изменён. Обновите данные.";
            }
            if (cause instanceof SQLException sql && sql.getSQLState() != null) {
                if (sql.getSQLState().startsWith("23")) {
                    status = 409;
                    message = "Изменение противоречит ограничениям БД. Обновите данные и проверьте связи.";
                } else if (sql.getSQLState().startsWith("22")) {
                    status = 400;
                    message = "Недопустимое значение или формат числового параметра";
                } else if (sql.getSQLState().startsWith("08")) {
                    status = 503;
                    message = "Нет соединения с базой данных. Повторите запрос позже.";
                }
            }
            if (cause instanceof jakarta.json.bind.JsonbException || cause instanceof NumberFormatException) {
                status = 400;
                message = "Некорректный формат: проверьте JSON, типы и диапазоны чисел";
            }
        }
        if (status >= 500) LOG.log(Level.SEVERE, "Request failed", error);
        return Response.status(status).type(MediaType.APPLICATION_JSON)
                .entity(Map.of("message", message == null ? "Ошибка запроса" : message, "fields", fields)).build();
    }
}
