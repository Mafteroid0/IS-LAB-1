package ru.itmo.routes.error;

public class Problem extends RuntimeException {
    public enum Kind { INVALID, NOT_FOUND, CONFLICT, UNAUTHORIZED }

    private final Kind kind;

    public Problem(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }
}
