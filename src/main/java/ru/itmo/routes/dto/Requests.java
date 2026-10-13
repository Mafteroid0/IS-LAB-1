package ru.itmo.routes.dto;

import jakarta.validation.constraints.*;
import ru.itmo.routes.validation.Finite;

public final class Requests {
    private Requests() {
    }

    public static class RouteInput {
        @NotBlank(message = "Укажите название маршрута")
        public String name;
        @NotNull @Positive
        public Long coordinatesId;
        @Positive
        public Long fromId;
        @NotNull @Positive
        public Long toId;
        @NotNull @Finite
        @DecimalMin(value = "1", inclusive = false, message = "Расстояние должно быть больше 1")
        public Float distance;
        @NotNull @Finite @Positive
        public Float rating;
        @PositiveOrZero
        public Long version;
    }

    public static class CoordinatesInput {
        @NotNull @Finite
        public Float x;
        @NotNull @Finite
        public Float y;
        @PositiveOrZero
        public Long version;
    }

    public static class LocationInput {
        @NotNull @Finite
        public Float x;
        @NotNull @Finite
        public Float y;
        @NotNull
        public Integer z;
        @NotBlank(message = "Укажите название локации")
        public String name;
        @PositiveOrZero
        public Long version;
    }

    public static class Credentials {
        @NotNull
        @Pattern(regexp = "[a-zA-Z0-9_]{3,32}", message = "Логин: 3–32 латинские буквы, цифры или _")
        public String username;
        @NotNull @Size(min = 8, max = 128, message = "Пароль: от 8 до 128 символов")
        public String password;
    }
}
