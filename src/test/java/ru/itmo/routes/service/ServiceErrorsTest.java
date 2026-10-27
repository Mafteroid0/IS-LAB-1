package ru.itmo.routes.service;

import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionalException;
import javax.transaction.xa.XAException;
import jakarta.validation.ConstraintViolationException;
import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.itmo.routes.error.*;
import static org.junit.jupiter.api.Assertions.*;

class ServiceErrorsTest {
    @ParameterizedTest
    @CsvSource({"23505, ConflictException", "23503, ConflictException", "23514, ConflictException",
            "22003, InvalidInputException", "22023, InvalidInputException",
            "08001, DatabaseUnavailableException", "08006, DatabaseUnavailableException",
            "40001, ConflictException", "40P01, ConflictException", "55P03, ConflictException",
            "42601, DatabaseException"})
    void translatesSqlFailuresInsideTransactionWrappers(String state, String type) {
        Exception original = new RollbackException("commit failed");
        original.initCause(new PersistenceException(new SQLException("private SQL detail", state)));
        Exception result = ServiceErrors.translate(original);
        assertEquals(type, result.getClass().getSimpleName());
        assertSame(original, result.getCause());
        assertFalse(result.getMessage().contains("private SQL detail"));
    }

    @Test
    void preservesBusinessErrorInsteadOfOverwritingItWithItsSqlCause() {
        var business = new ConflictException("Логин уже занят", new SQLException("offline", "08001"));
        assertSame(business, ServiceErrors.translate(new PersistenceException(business)));
    }

    @Test
    void preservesValidationDuringFlush() {
        var validation = new ConstraintViolationException(Set.of());
        assertSame(validation, ServiceErrors.translate(new PersistenceException(validation)));
    }

    @Test
    void handlesJdbcBatchExceptionChain() {
        var batch = new BatchUpdateException();
        batch.setNextException(new SQLException("duplicate", "23505"));
        assertInstanceOf(ConflictException.class, ServiceErrors.translate(new PersistenceException(batch)));
    }

    @Test
    void translatesOptimisticLocking() {
        assertInstanceOf(ConflictException.class,
                ServiceErrors.translate(new PersistenceException(new OptimisticLockException())));
    }

    @Test
    void leavesProgrammingErrorsToTheFallbackMapper() {
        var error = new NumberFormatException("invalid stored password hash");
        assertSame(error, ServiceErrors.translate(error));
    }

    @Test
    void handlesSqlExceptionWithoutState() {
        assertInstanceOf(DatabaseException.class, ServiceErrors.translate(new SQLException("internal")));
    }

    @Test
    void handlesCommitFailureWhenTheAdapterDiscardsTheSqlCause() {
        var system = new SystemException();
        system.initCause(new XAException("private adapter details"));
        var error = new TransactionalException("commit failed", system);
        var result = assertInstanceOf(DatabaseException.class, ServiceErrors.translate(error));
        assertSame(error, result.getCause());
        assertFalse(result.getMessage().contains("private adapter details"));
    }

    @Test
    void terminatesOnACyclicCauseChain() {
        var outer = new PersistenceException("outer");
        var inner = new RuntimeException(outer);
        outer.initCause(inner);
        assertInstanceOf(DatabaseException.class, ServiceErrors.translate(outer));
    }
}
