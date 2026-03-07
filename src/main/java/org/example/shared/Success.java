package org.example.shared;

import java.util.function.Function;

public record Success<T>(T payload) implements Result<T> {
    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public T getOrElse(T defaultValue) {
        return payload;
    }

    @Override
    public <U> Result<U> map(Function<T, U> fn) {
        return new Success<>(fn.apply(payload));
    }
}
