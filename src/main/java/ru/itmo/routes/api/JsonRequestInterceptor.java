package ru.itmo.routes.api;

import jakarta.json.bind.JsonbException;
import jakarta.json.stream.JsonParsingException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;
import java.io.IOException;

@Provider
public class JsonRequestInterceptor implements ReaderInterceptor {
    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException {
        try {
            return context.proceed();
        } catch (JsonbException | JsonParsingException error) {
            throw new BadRequestException(error);
        } catch (ProcessingException error) {
            if (error.getCause() instanceof JsonbException || error.getCause() instanceof JsonParsingException) {
                throw new BadRequestException(error);
            }
            throw error;
        }
    }
}
