package ru.itmo.routes.api;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.itmo.routes.dto.Views.ImportView;
import ru.itmo.routes.error.InvalidInputException;
import ru.itmo.routes.security.Roles;
import ru.itmo.routes.service.ImportService;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping(path = "/api/imports", produces = MediaType.APPLICATION_JSON_VALUE)
public class ImportController {
    private final ImportService imports;

    public ImportController(ImportService imports) {
        this.imports = imports;
    }

    /** Обычный пользователь видит только свои операции импорта, администратор — все. */
    @GetMapping
    public List<ImportView> history(Authentication current) {
        return imports.history(current.getName(), Roles.isAdmin(current));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportView> upload(@RequestParam("file") MultipartFile file, Authentication current)
            throws IOException {
        if (file.isEmpty()) throw new InvalidInputException("Выберите непустой файл");
        return ResponseEntity.status(201).body(imports.run(current.getName(), file.getBytes()));
    }
}
