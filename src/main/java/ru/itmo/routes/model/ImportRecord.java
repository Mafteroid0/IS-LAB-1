package ru.itmo.routes.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/** Запись истории импорта: создаётся для каждой попытки, успешной или нет. */
@Entity
@Table(name = "rl_import")
public class ImportRecord {
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rl_import_seq")
    @SequenceGenerator(name = "rl_import_seq", sequenceName = "rl_import_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false, length = 32)
    private String username;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "added_count")
    private Integer addedCount;

    @Column(columnDefinition = "text")
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ImportRecord() {
    }

    private ImportRecord(String username, String status, Integer addedCount, String message) {
        this.username = username;
        this.status = status;
        this.addedCount = addedCount;
        this.message = message;
    }

    public static ImportRecord success(String username, int added) {
        return new ImportRecord(username, SUCCESS, added, null);
    }

    public static ImportRecord failed(String username, String message) {
        return new ImportRecord(username, FAILED, null, message);
    }

    @PrePersist
    void created() {
        createdAt = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getStatus() {
        return status;
    }

    public Integer getAddedCount() {
        return addedCount;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
