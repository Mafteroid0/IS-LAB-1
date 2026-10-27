package ru.itmo.routes.api;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

final class ErrorResponses {
    static final String SERVER_ERROR = "Ошибка сервера. Повторите запрос позже.";
    private static final Logger LOG = Logger.getLogger(ErrorResponses.class.getName());

    private ErrorResponses() {
    }

    static Response create(int status, String message) {
        return create(status, message, Map.of());
    }

    static Response create(int status, String message, Map<String, String> fields) {
        return Response.status(status).type(MediaType.APPLICATION_JSON)
                .entity(body(message, fields)).build();
    }

    static Map<String, Object> body(String message, Map<String, String> fields) {
        return Map.of("message", message == null ? "Ошибка запроса" : message, "fields", fields);
    }

    static void log(Throwable error) {
        LOG.log(Level.SEVERE, "Request failed", error);
    }
}
