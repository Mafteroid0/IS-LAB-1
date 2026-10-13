package ru.itmo.routes.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import ru.itmo.routes.validation.Finite;
import org.hibernate.annotations.Check;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "rl_route")
@Check(constraints = "id > 0 and name ~ '[^[:space:]]' and distance > 1 and distance < 'Infinity'::real and rating > 0 and rating < 'Infinity'::real")
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rl_route_seq")
    @SequenceGenerator(name = "rl_route_seq", sequenceName = "rl_route_seq", allocationSize = 1)
    @NotNull
    @Positive
    private Long id;

    @Version
    private long version;

    @NotBlank(message = "Название маршрута не должно быть пустым")
    @Column(nullable = false, columnDefinition = "text")
    private String name;

    @NotNull
    @Valid
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coordinates_id", nullable = false)
    private Coordinates coordinates;

    @NotNull
    @Column(name = "creation_date", nullable = false, updatable = false)
    private LocalDateTime creationDate;

    @Valid
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_id")
    private Location from;

    @NotNull
    @Valid
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_id", nullable = false)
    private Location to;

    @NotNull
    @Finite
    @DecimalMin(value = "1", inclusive = false, message = "Расстояние должно быть больше 1")
    @Column(nullable = false)
    private Float distance;

    @Finite
    @Positive(message = "Рейтинг должен быть больше 0")
    @Column(nullable = false)
    private float rating;

    public Route() {
    }

    @PrePersist
    void created() {
        creationDate = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    public void change(String name, Coordinates coordinates, Location from, Location to,
                       Float distance, float rating) {
        this.name = name;
        this.coordinates = coordinates;
        this.from = from;
        this.to = to;
        this.distance = distance;
        this.rating = rating;
    }

    public void moveCoordinates(Coordinates replacement) {
        coordinates = replacement;
    }

    public void replaceLocation(long id, Location replacement) {
        if (from != null && from.getId() == id) from = replacement;
        if (to.getId() == id) to = replacement;
    }

    public Long getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public Location getFrom() {
        return from;
    }

    public Location getTo() {
        return to;
    }

    public Float getDistance() {
        return distance;
    }

    public float getRating() {
        return rating;
    }
}
