package ru.itmo.routes.error;

public class DatabaseException extends RuntimeException {
    public DatabaseException(Throwable cause) {
        super("Не удалось выполнить операцию с базой данных. Повторите запрос позже.", cause);
    }
}
