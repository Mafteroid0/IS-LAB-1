package ru.itmo.routes.service;

import ru.itmo.routes.error.Problem;
import static ru.itmo.routes.error.Problem.Kind.*;

public final class Checks {
    private Checks() {
    }

    public static <T> T found(T object, String label) {
        if (object == null) throw new Problem(NOT_FOUND, label + " не найден");
        return object;
    }

    public static void version(long actual, Long supplied) {
        if (supplied == null) throw new Problem(INVALID, "Укажите версию объекта");
        if (actual != supplied) throw new Problem(CONFLICT, "Объект уже изменён. Откройте его заново перед сохранением.");
    }
}
