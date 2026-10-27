package ru.itmo.routes.service;

import ru.itmo.routes.error.*;

public final class Checks {
    private Checks() {
    }

    public static <T> T found(T object, String label) {
        if (object == null) throw new ResourceNotFoundException(label + " не найден");
        return object;
    }

    public static void version(long actual, Long supplied) {
        if (supplied == null) throw new InvalidInputException("Укажите версию объекта");
        if (actual != supplied) throw new ConflictException("Объект уже изменён. Откройте его заново перед сохранением.");
    }
}
