package ru.itmo.routes.model;

import jakarta.persistence.*;

@Entity
@Table(name = "rl_state")
public class State {
    @Id
    private int id;

    @Column(nullable = false)
    private long revision;

    public long getRevision() {
        return revision;
    }

    public void advance() {
        revision++;
    }
}
