package org.example.shared;

import java.util.function.Function;

public record Failure<T>(String message) implements Result<T> {
    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public T getOrElse(T defaultValue) {
        return defaultValue;
    }

    @Override
    public <U> Result<U> map(Function<T, U> fn) {
        return new Failure<>(message);
    }
}
