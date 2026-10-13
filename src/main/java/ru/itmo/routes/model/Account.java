package ru.itmo.routes.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "rl_account")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rl_account_seq")
    @SequenceGenerator(name = "rl_account_seq", sequenceName = "rl_account_seq", allocationSize = 1)
    private long id;

    @NotNull
    @Pattern(regexp = "[a-zA-Z0-9_]{3,32}")
    @Column(nullable = false, unique = true, length = 32)
    private String username;

    @NotBlank
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    public Account() {
    }

    public Account(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
