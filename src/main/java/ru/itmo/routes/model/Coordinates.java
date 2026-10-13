package ru.itmo.routes.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import ru.itmo.routes.validation.Finite;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "rl_coordinates")
@Check(constraints = "id > 0 and x > '-Infinity'::real and x < 'Infinity'::real and y > '-Infinity'::real and y < 'Infinity'::real")
public class Coordinates {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rl_coordinates_seq")
    @SequenceGenerator(name = "rl_coordinates_seq", sequenceName = "rl_coordinates_seq", allocationSize = 1)
    @NotNull
    @Positive
    private Long id;

    @Version
    private long version;

    @Finite
    @Column(nullable = false)
    private float x;

    @NotNull
    @Finite
    @Column(nullable = false)
    private Float y;

    public Coordinates() {
    }

    public Coordinates(float x, Float y) {
        change(x, y);
    }

    public void change(float x, Float y) {
        this.x = x;
        this.y = y;
    }

    public Long getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public float getX() {
        return x;
    }

    public Float getY() {
        return y;
    }
}
