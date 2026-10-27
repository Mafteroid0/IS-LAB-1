package ru.itmo.routes.error;

public class DatabaseUnavailableException extends DatabaseException {
    public DatabaseUnavailableException(Throwable cause) {
        super(cause);
    }

    @Override
    public String getMessage() {
        return "Нет соединения с базой данных. Повторите запрос позже.";
    }
}
