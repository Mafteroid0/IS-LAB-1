package ru.itmo.routes.service;

import jakarta.annotation.Priority;
import jakarta.interceptor.InvocationContext;
import jakarta.transaction.RollbackException;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import ru.itmo.routes.error.ConflictException;
import static org.junit.jupiter.api.Assertions.*;

class ServiceErrorInterceptorTest {
    private final ServiceErrorInterceptor interceptor = new ServiceErrorInterceptor();

    @Test
    void translatesErrorsThrownByTransactionCompletion() {
        var rollback = new RollbackException("commit failed");
        rollback.initCause(new SQLException("deferred foreign key violation", "23503"));
        assertThrows(ConflictException.class, () -> interceptor.invoke(context(() -> { throw rollback; })));
        assertTrue(ServiceErrorInterceptor.class.getAnnotation(Priority.class).value() < 200);
    }

    @Test
    void returnsSuccessfulResultUnchanged() throws Exception {
        var value = new Object();
        assertSame(value, interceptor.invoke(context(() -> value)));
    }

    @Test
    void doesNotMaskUnexpectedFailures() {
        var failure = new IllegalStateException("bug");
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> interceptor.invoke(context(() -> { throw failure; }))));
    }

    private InvocationContext context(Callable<Object> action) {
        return (InvocationContext) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{InvocationContext.class}, (proxy, method, args) -> {
                    if (method.getName().equals("proceed")) return action.call();
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
