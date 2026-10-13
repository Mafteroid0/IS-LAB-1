package ru.itmo.routes.dto;

import ru.itmo.routes.model.*;
import java.util.List;
import jakarta.json.bind.annotation.JsonbNillable;

public final class Views {
    private Views() {
    }

    public record Coordinate(String id, float x, Float y, String version) {
        public static Coordinate of(Coordinates value) {
            return new Coordinate(value.getId().toString(), value.getX(), value.getY(),
                    Long.toString(value.getVersion()));
        }
    }

    public record Place(String id, Float x, float y, int z, String name, String version) {
        public static Place of(Location value) {
            return value == null ? null : new Place(value.getId().toString(), value.getX(), value.getY(),
                    value.getZ(), value.getName(), Long.toString(value.getVersion()));
        }
    }

    @JsonbNillable
    public record RouteView(String id, String name, Coordinate coordinates, String creationDate,
                            Place from, Place to, Float distance, float rating, String version) {
        public static RouteView of(Route value) {
            return new RouteView(value.getId().toString(), value.getName(), Coordinate.of(value.getCoordinates()),
                    value.getCreationDate().toString(), Place.of(value.getFrom()), Place.of(value.getTo()),
                    value.getDistance(), value.getRating(), Long.toString(value.getVersion()));
        }
    }

    public record Page<T>(List<T> items, long total, int page, int size) {
    }

    @JsonbNillable
    public record Average(Double value) {
    }
}
