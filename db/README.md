# Миграции базы данных

Схема и функции перенесены из `db/schema.sql` и `db/functions.sql` в:

- `src/main/resources/db/migration/V1__create_schema.sql` — таблицы, последовательности, индексы и начальная ревизия;
- `src/main/resources/db/migration/V2__create_functions.sql` — SQL-функции специальных операций.

- `src/main/resources/db/migration/V3__create_import_history.sql` — таблица истории импорта `rl_import`.

Ограничений уникальности для названий и координат в миграциях нет намеренно: они проверяются в бизнес-логике.

Миграции применяются автоматически при запуске приложения (Flyway встроен в Spring Boot).
Flyway сам управляет транзакцией каждого файла и хранит версии и контрольные
суммы в `flyway_schema_history`. Отдельные `BEGIN`/`COMMIT` в миграциях не нужны.
Не запускайте файлы вручную через `psql`.

Настройка подключения и переход с существующей БД описаны в [README](../README.md).
