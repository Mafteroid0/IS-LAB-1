package ru.itmo.routes.api;

import jakarta.json.bind.JsonbException;
import jakarta.json.stream.JsonParsingException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.stream.Stream;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.itmo.routes.dto.Requests;
import ru.itmo.routes.error.*;
import static org.junit.jupiter.api.Assertions.*;

class ErrorMappersTest {
    private static final ValidatorFactory VALIDATORS = Validation.byDefaultProvider().configure()
            .messageInterpolator(new ParameterMessageInterpolator()).buildValidatorFactory();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATORS.close();
    }

    static Stream<Arguments> businessResponses() {
        return Stream.of(
                Arguments.of(new InvalidInputErrorMapper().toResponse(new InvalidInputException("invalid")), 400, "invalid"),
                Arguments.of(new ResourceNotFoundErrorMapper().toResponse(new ResourceNotFoundException("missing")), 404, "missing"),
                Arguments.of(new ConflictErrorMapper().toResponse(new ConflictException("conflict")), 409, "conflict"),
                Arguments.of(new UnauthorizedErrorMapper().toResponse(new UnauthorizedException("sign in")), 401, "sign in"));
    }

    @ParameterizedTest
    @MethodSource("businessResponses")
    void keepsBusinessStatusAndMessage(Response response, int status, String message) {
        assertResponse(response, status, message);
    }

    @Test
    void reportsInvalidFields() {
        var input = new Requests.CoordinatesInput();
        input.x = Float.NaN;
        var violations = VALIDATORS.getValidator().validate(input);
        Response response = new ValidationErrorMapper().toResponse(new ConstraintViolationException(violations));
        assertEquals(400, response.getStatus());
        var fields = (Map<?, ?>) body(response).get("fields");
        assertEquals(2, fields.size());
        assertTrue(fields.containsKey("x"));
        assertTrue(fields.containsKey("y"));
    }

    @Test
    void treatsInvalidReturnValueAsServerFailure() throws Exception {
        var violations = VALIDATORS.getValidator().forExecutables().validateReturnValue(
                new InvalidService(), InvalidService.class.getMethod("result"), null);
        assertResponse(new ValidationErrorMapper().toResponse(new ConstraintViolationException(violations)),
                500, ErrorResponses.SERVER_ERROR);
    }

    @Test
    void handlesBothJsonParsersAndHttpWrappedJsonErrors() {
        var json = new JsonbException("internal class name");
        for (Response response : new Response[]{new JsonbErrorMapper().toResponse(json),
                new JsonParsingErrorMapper().toResponse(new JsonParsingException("private input", null)),
                new HttpErrorMapper().toResponse(new BadRequestException(json))}) {
            assertEquals(400, response.getStatus());
            assertFalse(body(response).get("message").toString().contains("internal class name"));
            assertFalse(body(response).get("message").toString().contains("private input"));
        }
    }

    @Test
    void preservesHttpHeaders() {
        Response response = new HttpErrorMapper().toResponse(new NotAllowedException("GET", new String[]{"POST"}));
        assertEquals(405, response.getStatus());
        assertTrue(response.getAllowedMethods().containsAll(java.util.Set.of("GET", "POST")));
        response = new HttpErrorMapper().toResponse(new WebApplicationException(
                Response.status(401).header("WWW-Authenticate", "Basic realm=routes").build()));
        assertEquals(401, response.getStatus());
        assertEquals("Basic realm=routes", response.getHeaderString("WWW-Authenticate"));
    }

    @Test
    void hidesServerAndDatabaseDetails() {
        var internal = new IllegalStateException("password=secret; SELECT * FROM private_table");
        assertResponse(new ErrorMapper().toResponse(internal), 500, ErrorResponses.SERVER_ERROR);
        assertResponse(new HttpErrorMapper().toResponse(new WebApplicationException("private", 500)),
                500, ErrorResponses.SERVER_ERROR);
        Response database = new DatabaseErrorMapper().toResponse(new DatabaseException(internal));
        assertEquals(500, database.getStatus());
        assertFalse(body(database).get("message").toString().contains("secret"));
        assertEquals(503, new DatabaseUnavailableErrorMapper()
                .toResponse(new DatabaseUnavailableException(internal)).getStatus());
    }

    @Test
    void fallbackDoesNotGuessTypesFromNestedCauses() {
        assertResponse(new ErrorMapper().toResponse(new RuntimeException(new InvalidInputException("nested"))),
                500, ErrorResponses.SERVER_ERROR);
    }

    private static void assertResponse(Response response, int status, String message) {
        assertEquals(status, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        assertEquals(message, body(response).get("message"));
        assertEquals(Map.of(), body(response).get("fields"));
    }

    private static Map<?, ?> body(Response response) {
        return (Map<?, ?>) response.getEntity();
    }

    public static class InvalidService {
        @NotNull
        public String result() { return null; }
    }
}
