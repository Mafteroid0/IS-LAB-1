package ru.itmo.routes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import ru.itmo.routes.error.ImportRejectedException;
import ru.itmo.routes.error.InvalidInputException;
import ru.itmo.routes.model.ImportRecord;
import ru.itmo.routes.repository.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ImportServiceTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private RouteRepository routes;
    private ImportRepository history;
    private WriteCoordinator changes;
    private ImportService service;

    @BeforeEach
    void setUp() {
        routes = mock(RouteRepository.class);
        history = mock(ImportRepository.class);
        changes = mock(WriteCoordinator.class);
        service = new ImportService(new ObjectMapper(), validator, mock(PlatformTransactionManager.class), routes,
                mock(CoordinatesRepository.class), mock(LocationRepository.class), history, changes,
                mock(UniquenessRules.class));
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private ImportRecord savedFailure() {
        ArgumentCaptor<ImportRecord> captor = ArgumentCaptor.forClass(ImportRecord.class);
        verify(history).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void brokenJsonIsRejectedAndRecordedAsFailedWithoutTouchingData() {
        assertThrows(InvalidInputException.class, () -> service.run("alice", bytes("[{")));
        ImportRecord record = savedFailure();
        assertEquals(ImportRecord.FAILED, record.getStatus());
        assertEquals("alice", record.getUsername());
        assertNull(record.getAddedCount());
        verifyNoInteractions(changes);
        verify(routes, never()).saveAndFlush(any());
    }

    @Test
    void unknownFieldsAndEmptyArrayAreRejected() {
        assertThrows(InvalidInputException.class, () -> service.run("alice", bytes("[{\"surprise\":1}]")));
        assertThrows(InvalidInputException.class, () -> service.run("alice", bytes("[]")));
        verify(routes, never()).saveAndFlush(any());
    }

    @Test
    void domainConstraintsAreCheckedForEveryRecordIncludingNestedObjects() {
        String file = "[{\"name\":\" \",\"coordinates\":{\"x\":1,\"y\":2},\"to\":{\"x\":1,\"y\":1,\"z\":1,\"name\":\"B\"},"
                + "\"distance\":0.5,\"rating\":-1},{\"name\":\"ok\",\"coordinates\":{\"x\":1},\"to\":{\"x\":1,\"y\":1,\"z\":1,\"name\":\" \"},"
                + "\"distance\":5,\"rating\":1}]";
        ImportRejectedException error = assertThrows(ImportRejectedException.class, () -> service.run("bob", bytes(file)));
        assertTrue(error.fields().containsKey("запись 1: name"), error.fields().toString());
        assertTrue(error.fields().containsKey("запись 1: distance"));
        assertTrue(error.fields().containsKey("запись 1: rating"));
        assertTrue(error.fields().containsKey("запись 2: coordinates.y"));
        assertTrue(error.fields().containsKey("запись 2: to.name"));
        assertEquals(ImportRecord.FAILED, savedFailure().getStatus());
        verify(routes, never()).saveAndFlush(any());
        verifyNoInteractions(changes);
    }

    @Test
    void nullRecordIsReportedAndRequiredNestedObjectsAreMandatory() {
        ImportRejectedException error = assertThrows(ImportRejectedException.class,
                () -> service.run("bob", bytes("[null,{\"name\":\"x\",\"distance\":5,\"rating\":1}]")));
        assertEquals("Запись пуста", error.fields().get("запись 1"));
        assertTrue(error.fields().containsKey("запись 2: coordinates"));
        assertTrue(error.fields().containsKey("запись 2: to"));
    }
}
