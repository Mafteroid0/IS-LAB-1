package ru.itmo.routes.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import ru.itmo.routes.validation.Finite;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "rl_location")
@Check(constraints = "id > 0 and x > '-Infinity'::real and x < 'Infinity'::real and y > '-Infinity'::real and y < 'Infinity'::real and name ~ '[^[:space:]]'")
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rl_location_seq")
    @SequenceGenerator(name = "rl_location_seq", sequenceName = "rl_location_seq", allocationSize = 1)
    @NotNull
    @Positive
    private Long id;

    @Version
    private long version;

    @NotNull
    @Finite
    @Column(nullable = false)
    private Float x;

    @Finite
    @Column(nullable = false)
    private float y;

    @Column(nullable = false)
    private int z;

    @NotBlank(message = "Название локации не должно быть пустым")
    @Column(nullable = false, columnDefinition = "text")
    private String name;

    public Location() {
    }

    public Location(Float x, float y, int z, String name) {
        change(x, y, z, name);
    }

    public void change(Float x, float y, int z, String name) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public Float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public String getName() {
        return name;
    }
}
