package ru.itmo.routes.api;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Единый формат ошибок API: {"message": "...", "fields": {}}. */
public final class ErrorResponses {
    public static final String SERVER_ERROR = "Ошибка сервера. Повторите запрос позже.";
    private static final Logger LOG = Logger.getLogger(ErrorResponses.class.getName());

    private ErrorResponses() {
    }

    public static Map<String, Object> body(String message, Map<String, String> fields) {
        return Map.of("message", message == null ? "Ошибка запроса" : message, "fields", fields);
    }

    public static Map<String, Object> body(String message) {
        return body(message, Map.of());
    }

    static void log(Throwable error) {
        LOG.log(Level.SEVERE, "Request failed", error);
    }
}
