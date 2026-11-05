package ru.itmo.routes.api;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import ru.itmo.routes.error.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Преобразует исключения контроллеров в ответы формата {@link ErrorResponses}: стандартные ошибки
 * Spring MVC (JSON, параметры, методы) обрабатывает базовый класс, остальные проходят через
 * {@link ServiceErrors}. Детали сервера и БД клиенту не передаются.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    static final String BAD_REQUEST = "Некорректный запрос: проверьте JSON, параметры, типы и диапазоны чисел";
    static final String BAD_JSON = "Некорректный JSON: проверьте синтаксис, типы и диапазоны чисел";

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleApplication(Exception error) {
        Exception cause = ServiceErrors.translate(error);
        if (cause instanceof InvalidInputException) return reply(400, cause.getMessage());
        if (cause instanceof ResourceNotFoundException) return reply(404, cause.getMessage());
        if (cause instanceof ConflictException) return reply(409, cause.getMessage());
        if (cause instanceof UnauthorizedException) return reply(401, cause.getMessage());
        if (cause instanceof ConstraintViolationException validation) return validation(validation);
        if (cause instanceof DatabaseUnavailableException) {
            ErrorResponses.log(cause);
            return reply(503, cause.getMessage());
        }
        if (cause instanceof DatabaseException) {
            ErrorResponses.log(cause);
            return reply(500, cause.getMessage());
        }
        ErrorResponses.log(cause);
        return reply(500, ErrorResponses.SERVER_ERROR);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException error,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError field : error.getBindingResult().getFieldErrors())
            fields.putIfAbsent(field.getField(), field.getDefaultMessage());
        return reply(400, "Исправьте значения полей", fields);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception error, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        int code = status.value();
        String message = error instanceof HttpMessageNotReadableException ? BAD_JSON : switch (code) {
            case 400 -> BAD_REQUEST;
            case 404 -> "Ресурс не найден";
            case 405 -> "Метод запроса не поддерживается";
            case 415 -> "Неподдерживаемый формат данных. Используйте application/json";
            default -> HttpStatus.valueOf(code).getReasonPhrase();
        };
        if (code >= 500) {
            ErrorResponses.log(error);
            message = ErrorResponses.SERVER_ERROR;
        }
        HttpHeaders result = new HttpHeaders();
        if (headers != null) result.putAll(headers);
        result.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(ErrorResponses.body(message), result, status);
    }

    private ResponseEntity<Object> validation(ConstraintViolationException error) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : error.getConstraintViolations()) {
            for (Path.Node node : violation.getPropertyPath()) {
                if (node.getKind() == ElementKind.RETURN_VALUE) {
                    ErrorResponses.log(error);
                    return reply(500, ErrorResponses.SERVER_ERROR);
                }
            }
            String path = violation.getPropertyPath().toString();
            fields.putIfAbsent(path.substring(path.lastIndexOf('.') + 1), violation.getMessage());
        }
        return reply(400, "Исправьте значения полей", fields);
    }

    private static ResponseEntity<Object> reply(int status, String message) {
        return reply(status, message, Map.of());
    }

    private static ResponseEntity<Object> reply(int status, String message, Map<String, String> fields) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponses.body(message, fields));
    }
}
