package ru.itmo.routes.error;

import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PessimisticLockException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Преобразует ошибки JPA/JDBC/Spring в исключения приложения. Тип определяется по классам и SQLState
 * в цепочке причин, но не по тексту сообщений; SQL-детали наружу не передаются.
 */
public final class ServiceErrors {
    private ServiceErrors() {
    }

    public static Exception translate(Exception error) {
        List<Throwable> causes = causes(error);
        for (Throwable cause : causes) {
            if (cause instanceof BusinessException business) return business;
            if (cause instanceof DatabaseException database) return database;
            if (cause instanceof ConstraintViolationException validation) return validation;
        }
        for (Throwable cause : causes) {
            if (cause instanceof OptimisticLockException
                    || cause instanceof PessimisticLockException
                    || cause instanceof LockTimeoutException
                    || cause instanceof OptimisticLockingFailureException
                    || cause instanceof PessimisticLockingFailureException) {
                return new ConflictException("Объект уже изменён или занят другим запросом. Обновите данные.", error);
            }
            if (cause instanceof SQLException sql && sql.getSQLState() != null) {
                String state = sql.getSQLState();
                if (state.startsWith("23")) {
                    return new ConflictException(
                            "Изменение противоречит ограничениям БД. Обновите данные и проверьте связи.", error);
                }
                if (state.startsWith("22")) {
                    return new InvalidInputException("Недопустимое значение или формат числового параметра", error);
                }
                if (state.startsWith("08")) return new DatabaseUnavailableException(error);
                if (state.startsWith("40") || state.equals("55P03")) {
                    return new ConflictException("Данные изменяются другим запросом. Повторите операцию.", error);
                }
            }
        }
        if (causes.stream().anyMatch(c -> c instanceof DataAccessResourceFailureException
                || c instanceof CannotGetJdbcConnectionException
                || c instanceof CannotCreateTransactionException)) {
            return new DatabaseUnavailableException(error);
        }
        if (causes.stream().anyMatch(c -> c instanceof DataIntegrityViolationException)) {
            return new ConflictException(
                    "Изменение противоречит ограничениям БД. Обновите данные и проверьте связи.", error);
        }
        if (causes.stream().anyMatch(c -> c instanceof PersistenceException || c instanceof SQLException
                || c instanceof DataAccessException || c instanceof TransactionException)) {
            return new DatabaseException(error);
        }
        return error;
    }

    private static List<Throwable> causes(Throwable error) {
        List<Throwable> result = new ArrayList<>();
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        collect(error, visited, result);
        return result;
    }

    private static void collect(Throwable error, Set<Throwable> visited, List<Throwable> result) {
        if (error == null || !visited.add(error)) return;
        result.add(error);
        collect(error.getCause(), visited, result);
        if (error instanceof SQLException sql) collect(sql.getNextException(), visited, result);
    }
}
