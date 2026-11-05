package ru.itmo.routes.repository;

import ru.itmo.routes.model.Route;
import java.util.List;

/** Динамические запросы списка маршрутов; реализация подключается Spring Data автоматически (RouteQueriesImpl). */
public interface RouteQueries {
    List<Route> list(String name, String fromName, String toName, String sort,
                     boolean descending, int offset, int limit);

    long count(String name, String fromName, String toName);
}
