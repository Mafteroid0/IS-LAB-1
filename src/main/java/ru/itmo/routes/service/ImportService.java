package ru.itmo.routes.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import ru.itmo.routes.dto.Requests.CoordinatesInput;
import ru.itmo.routes.dto.Requests.LocationInput;
import ru.itmo.routes.dto.Requests.RouteImport;
import ru.itmo.routes.dto.Views.ImportView;
import ru.itmo.routes.error.*;
import ru.itmo.routes.model.*;
import ru.itmo.routes.repository.*;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Массовый импорт маршрутов из JSON-массива. Вложенные объекты (coordinates, from, to) задаются в той же записи.
 * Все записи сохраняются в одной транзакции: при любой ошибке не создаётся ни один объект. Попытка импорта
 * фиксируется в истории: успешная — внутри той же транзакции, неуспешная — отдельной транзакцией после отката.
 * Координаты и локации, совпадающие с существующими по ключу уникальности, переиспользуются.
 */
@Service
public class ImportService {
    public static final int MAX_RECORDS = 1000;
    private static final int MAX_ERRORS = 20;
    private static final int MAX_MESSAGE = 1000;
    private static final String INTERNAL = "Внутренняя ошибка сервера";
    private static final Logger LOG = Logger.getLogger(ImportService.class.getName());

    private final ObjectMapper mapper;
    private final Validator validator;
    private final TransactionTemplate atomic;
    private final TransactionTemplate separate;
    private final RouteRepository routes;
    private final CoordinatesRepository coordinates;
    private final LocationRepository locations;
    private final ImportRepository history;
    private final WriteCoordinator changes;
    private final UniquenessRules rules;

    public ImportService(ObjectMapper mapper, Validator validator, PlatformTransactionManager transactions,
                         RouteRepository routes, CoordinatesRepository coordinates, LocationRepository locations,
                         ImportRepository history, WriteCoordinator changes, UniquenessRules rules) {
        this.mapper = mapper.copy()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        this.validator = validator;
        this.routes = routes;
        this.coordinates = coordinates;
        this.locations = locations;
        this.history = history;
        this.changes = changes;
        this.rules = rules;
        // READ COMMITTED: первая же команда транзакции берёт глобальную блокировку записи и только после неё
        // выполняются проверки, поэтому они видят все зафиксированные изменения (см. WriteCoordinator).
        this.atomic = new TransactionTemplate(transactions);
        this.atomic.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        this.separate = new TransactionTemplate(transactions);
        this.separate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public ImportView run(String username, byte[] content) {
        try {
            List<RouteImport> items = parse(content);
            check(items);
            return atomic.execute(status -> store(username, items));
        } catch (RuntimeException error) {
            recordFailure(username, error);
            throw error;
        }
    }

    public List<ImportView> history(String username, boolean all) {
        List<ImportRecord> records = all ? history.findTop200ByOrderByIdDesc()
                : history.findTop200ByUsernameOrderByIdDesc(username);
        return records.stream().map(ImportView::of).toList();
    }

    private List<RouteImport> parse(byte[] content) {
        try {
            return mapper.readValue(content, new TypeReference<List<RouteImport>>() {
            });
        } catch (IOException | RuntimeException error) {
            throw new InvalidInputException("Файл не является корректным JSON-массивом маршрутов", error);
        }
    }

    private void check(List<RouteImport> items) {
        if (items.isEmpty()) throw new InvalidInputException("Файл не содержит ни одной записи");
        if (items.size() > MAX_RECORDS)
            throw new InvalidInputException("В файле больше " + MAX_RECORDS + " записей");
        Map<String, String> fields = new LinkedHashMap<>();
        for (int index = 0; index < items.size() && fields.size() < MAX_ERRORS; index++) {
            String prefix = "запись " + (index + 1);
            RouteImport item = items.get(index);
            if (item == null) {
                fields.put(prefix, "Запись пуста");
                continue;
            }
            for (ConstraintViolation<RouteImport> violation : validator.validate(item)) {
                if (fields.size() >= MAX_ERRORS) break;
                fields.putIfAbsent(prefix + ": " + violation.getPropertyPath(), violation.getMessage());
            }
        }
        if (!fields.isEmpty()) throw new ImportRejectedException("Импорт отклонён: исправьте данные в файле", fields);
    }

    private ImportView store(String username, List<RouteImport> items) {
        changes.changed();
        int number = 0;
        for (RouteImport item : items) {
            number++;
            try {
                add(item);
            } catch (ConflictException error) {
                throw new ConflictException("Запись " + number + ": " + error.getMessage(), error);
            }
        }
        return ImportView.of(history.save(ImportRecord.success(username, items.size())));
    }

    private void add(RouteImport item) {
        rules.routeName(item.name, 0);
        Coordinates point = coordinatesFor(item.coordinates);
        Location from = item.from == null ? null : locationFor(item.from);
        Location to = locationFor(item.to);
        Route route = new Route();
        route.change(item.name, point, from, to, item.distance, item.rating);
        routes.saveAndFlush(route);
    }

    private Coordinates coordinatesFor(CoordinatesInput input) {
        List<Coordinates> same = rules.sameCoordinates(input.x, input.y);
        if (!same.isEmpty()) return same.get(0);
        return coordinates.saveAndFlush(new Coordinates(input.x, input.y));
    }

    private Location locationFor(LocationInput input) {
        List<Location> same = rules.sameLocations(input.name);
        if (same.isEmpty()) return locations.saveAndFlush(new Location(input.x, input.y, input.z, input.name));
        Location existing = same.get(0);
        if (existing.getX().equals(input.x) && existing.getY() == input.y && existing.getZ() == input.z)
            return existing;
        throw new ConflictException("Локация «" + input.name.strip() + "» уже существует с другими параметрами");
    }

    private void recordFailure(String username, RuntimeException error) {
        Exception cause = ServiceErrors.translate(error);
        String message = cause instanceof BusinessException ? describe(cause) : INTERNAL;
        if (!(cause instanceof BusinessException)) LOG.log(Level.SEVERE, "Import failed", error);
        String saved = message.length() > MAX_MESSAGE ? message.substring(0, MAX_MESSAGE) : message;
        try {
            separate.executeWithoutResult(status -> history.save(ImportRecord.failed(username, saved)));
        } catch (RuntimeException historyError) {
            LOG.log(Level.SEVERE, "Import history was not saved", historyError);
        }
    }

    private static String describe(Exception cause) {
        if (cause instanceof ImportRejectedException rejected && !rejected.fields().isEmpty())
            return rejected.getMessage() + " — " + rejected.fields().entrySet().stream().limit(5)
                    .map(entry -> entry.getKey() + ": " + entry.getValue()).collect(Collectors.joining("; "));
        return cause.getMessage();
    }
}
