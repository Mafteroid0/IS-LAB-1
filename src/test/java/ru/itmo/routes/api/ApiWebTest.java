package ru.itmo.routes.api;

import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.mock.web.MockMultipartFile;
import ru.itmo.routes.dto.Views.ImportView;
import ru.itmo.routes.error.ConflictException;
import ru.itmo.routes.error.ImportRejectedException;
import ru.itmo.routes.security.Roles;
import ru.itmo.routes.service.ImportService;
import java.util.List;
import java.util.Map;
import ru.itmo.routes.error.ResourceNotFoundException;
import ru.itmo.routes.model.Account;
import ru.itmo.routes.repository.AccountRepository;
import ru.itmo.routes.security.AccountUserDetailsService;
import ru.itmo.routes.security.SecurityConfig;
import ru.itmo.routes.service.AuthService;
import ru.itmo.routes.service.Passwords;
import ru.itmo.routes.service.RouteService;
import ru.itmo.routes.service.WriteCoordinator;
import java.sql.SQLException;
import java.util.Optional;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {RouteController.class, AuthController.class, StateController.class, ImportController.class})
@Import({SecurityConfig.class, AccountUserDetailsService.class, Roles.class})
@ImportAutoConfiguration(MessageSourceAutoConfiguration.class)
class ApiWebTest {
    @Autowired MockMvc mvc;
    @MockitoBean RouteService routes;
    @MockitoBean AuthService auth;
    @MockitoBean WriteCoordinator changes;
    @MockitoBean AccountRepository accounts;
    @MockitoBean ImportService imports;

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.header("X-Requested-With", "RouteLab").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void requiresSignInForApi() throws Exception {
        mvc.perform(get("/api/routes/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Войдите в систему"))
                .andExpect(jsonPath("$.fields").isMap());
    }

    @Test
    void servesStaticPagesWithoutSignIn() throws Exception {
        mvc.perform(get("/index.html")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void rejectsChangingRequestWithoutRequestedWithHeader() throws Exception {
        mvc.perform(post("/api/routes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("X-Requested-With")));
    }

    @Test
    void signsInAndKeepsSession() throws Exception {
        when(accounts.findByUsername("alice")).thenReturn(Optional.of(
                new Account("alice", Passwords.hash("password123"))));
        MvcResult login = mvc.perform(json(post("/api/auth/login"),
                        "{\"username\":\"alice\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
        mvc.perform(json(post("/api/auth/logout"), "").session(session)).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsWrongPasswordAndUnknownUserWithTheSameMessage() throws Exception {
        when(accounts.findByUsername("alice")).thenReturn(Optional.of(
                new Account("alice", Passwords.hash("password123"))));
        for (String user : new String[]{"alice", "nobody"}) {
            mvc.perform(json(post("/api/auth/login"), "{\"username\":\"" + user + "\",\"password\":\"wrong-password\"}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Неверный логин или пароль"));
        }
    }

    @Test
    void registrationStartsSession() throws Exception {
        when(auth.register(any())).thenReturn("bob");
        MvcResult result = mvc.perform(json(post("/api/auth/register"),
                        "{\"username\":\"bob\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("bob"))
                .andReturn();
        mvc.perform(get("/api/auth/me").session((MockHttpSession) result.getRequest().getSession(false)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void reportsInvalidBodyFieldsInRussian() throws Exception {
        mvc.perform(json(post("/api/routes"), "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Исправьте значения полей"))
                .andExpect(jsonPath("$.fields.name").value("Укажите название маршрута"))
                .andExpect(jsonPath("$.fields.coordinatesId").value("Поле обязательно"))
                .andExpect(jsonPath("$.fields.toId").exists())
                .andExpect(jsonPath("$.fields.distance").exists())
                .andExpect(jsonPath("$.fields.rating").exists());
    }

    @Test
    @WithMockUser
    void validatesPathAndQueryParameters() throws Exception {
        mvc.perform(get("/api/routes/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.id").value("Значение должно быть больше 0"));
        mvc.perform(get("/api/routes/abc")).andExpect(status().isBadRequest());
        mvc.perform(json(delete("/api/routes/1"), ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.version").value("Поле обязательно"));
    }

    @Test
    @WithMockUser
    void hidesJsonParserDetails() throws Exception {
        mvc.perform(json(post("/api/routes"), "{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ApiExceptionHandler.BAD_JSON));
        mvc.perform(json(post("/api/routes"), "{\"distance\":\"text\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(not(containsString("Float"))));
    }

    @Test
    @WithMockUser
    void mapsBusinessErrorsToStatuses() throws Exception {
        when(routes.get(4)).thenThrow(new ResourceNotFoundException("Маршрут #4 не найден"));
        when(routes.get(5)).thenThrow(new ConflictException("Объект уже изменён"));
        mvc.perform(get("/api/routes/4")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Маршрут #4 не найден"));
        mvc.perform(get("/api/routes/5")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Объект уже изменён"));
    }

    @Test
    @WithMockUser
    void translatesDatabaseFailuresWithoutLeakingDetails() throws Exception {
        when(routes.get(7)).thenThrow(new TransactionSystemException("commit",
                new PersistenceException(new SQLException("secret SQL text", "23505"))));
        when(routes.get(8)).thenThrow(new IllegalStateException("password=secret"));
        mvc.perform(get("/api/routes/7")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(not(containsString("secret"))));
        mvc.perform(get("/api/routes/8")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(ErrorResponses.SERVER_ERROR));
    }

    @Test
    @WithMockUser
    void keepsAllowHeaderAndJsonFormatForUnsupportedMethod() throws Exception {
        mvc.perform(json(delete("/api/state"), ""))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.message").value("Метод запроса не поддерживается"));
    }

    @Test
    @WithMockUser
    void answersUnknownPathWithJsonNotFound() throws Exception {
        mvc.perform(get("/api/unknown")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ресурс не найден"));
    }

    private static MockMultipartFile file(String content) {
        return new MockMultipartFile("file", "routes.json", "application/json", content.getBytes());
    }

    @Test
    @WithMockUser(username = "alice")
    void uploadsImportFileAndReturnsHistoryRow() throws Exception {
        when(imports.run(eq("alice"), any())).thenReturn(
                new ImportView("7", "SUCCESS", "alice", 3, null, "2026-01-01T00:00:00"));
        mvc.perform(multipart("/api/imports").file(file("[]")).header("X-Requested-With", "RouteLab"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("7"))
                .andExpect(jsonPath("$.added").value(3));
    }

    @Test
    @WithMockUser(username = "alice")
    void reportsRejectedImportFieldsAndEmptyFile() throws Exception {
        when(imports.run(eq("alice"), any())).thenThrow(new ImportRejectedException("Импорт отклонён",
                Map.of("запись 2: name", "Укажите название маршрута")));
        mvc.perform(multipart("/api/imports").file(file("[{}]")).header("X-Requested-With", "RouteLab"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Импорт отклонён"))
                .andExpect(jsonPath("$.fields['запись 2: name']").value("Укажите название маршрута"));
        mvc.perform(multipart("/api/imports").file(file("")).header("X-Requested-With", "RouteLab"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Выберите непустой файл"));
    }

    @Test
    @WithMockUser(username = "alice")
    void rejectsImportWithoutRequestedWithHeader() throws Exception {
        mvc.perform(multipart("/api/imports").file(file("[]"))).andExpect(status().isForbidden());
    }

    @Test
    void importHistoryRequiresSignIn() throws Exception {
        mvc.perform(get("/api/imports")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "alice")
    void ordinaryUserAsksForOwnHistoryOnly() throws Exception {
        when(imports.history("alice", false)).thenReturn(List.of(
                new ImportView("1", "FAILED", "alice", null, "ошибка", "2026-01-01T00:00:00")));
        mvc.perform(get("/api/imports")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[0].added").doesNotExist());
        verify(imports).history("alice", false);
    }

    @Test
    @WithMockUser(username = "boss", roles = "ADMIN")
    void administratorAsksForAllHistory() throws Exception {
        when(imports.history("boss", true)).thenReturn(List.of());
        mvc.perform(get("/api/imports")).andExpect(status().isOk());
        verify(imports).history("boss", true);
    }

    @Test
    void loginReportsRoleFromConfiguration() throws Exception {
        when(accounts.findByUsername("admin")).thenReturn(Optional.of(
                new Account("admin", Passwords.hash("password123"))));
        mvc.perform(json(post("/api/auth/login"), "{\"username\":\"admin\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
        when(accounts.findByUsername("alice")).thenReturn(Optional.of(
                new Account("alice", Passwords.hash("password123"))));
        mvc.perform(json(post("/api/auth/login"), "{\"username\":\"alice\",\"password\":\"password123\"}"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @WithMockUser
    void exposesStateRevision() throws Exception {
        when(changes.revision()).thenReturn("12");
        mvc.perform(get("/api/state")).andExpect(status().isOk())
                .andExpect(jsonPath("$.revision").value("12"));
    }
}
