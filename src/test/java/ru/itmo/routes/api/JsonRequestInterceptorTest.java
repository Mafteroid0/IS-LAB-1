package ru.itmo.routes.api;

import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.ext.ReaderInterceptorContext;
import java.lang.reflect.Proxy;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JsonRequestInterceptorTest {
    private final JsonRequestInterceptor interceptor = new JsonRequestInterceptor();

    @Test
    void normalizesJerseyJsonBindingFailure() {
        var failure = new ProcessingException("Error deserializing object", new JsonbException("invalid JSON"));
        var result = assertThrows(BadRequestException.class,
                () -> interceptor.aroundReadFrom(context(() -> { throw failure; })));
        assertSame(failure, result.getCause());
    }

    @Test
    void doesNotMisclassifyOtherProcessingErrorsAsInvalidInput() {
        var failure = new ProcessingException("internal reader failure");
        assertSame(failure, assertThrows(ProcessingException.class,
                () -> interceptor.aroundReadFrom(context(() -> { throw failure; }))));
    }

    @Test
    void passesParsedBodyThrough() throws Exception {
        var value = new Object();
        assertSame(value, interceptor.aroundReadFrom(context(() -> value)));
    }

    private ReaderInterceptorContext context(Callable<Object> action) {
        return (ReaderInterceptorContext) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{ReaderInterceptorContext.class}, (proxy, method, args) -> {
                    if (method.getName().equals("proceed")) return action.call();
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
