package ru.itmo.routes.error;

import java.util.Map;

/** Файл импорта отклонён из-за некорректных данных; fields — сообщения по записям и полям. */
public class ImportRejectedException extends InvalidInputException {
    private final Map<String, String> fields;

    public ImportRejectedException(String message, Map<String, String> fields) {
        super(message);
        this.fields = Map.copyOf(fields);
    }

    public Map<String, String> fields() {
        return fields;
    }
}
