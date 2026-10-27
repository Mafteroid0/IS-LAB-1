package ru.itmo.routes.service;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@TranslateServiceErrors
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 100)
public class ServiceErrorInterceptor {
    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        try {
            return context.proceed();
        } catch (Exception error) {
            throw ServiceErrors.translate(error);
        }
    }
}
