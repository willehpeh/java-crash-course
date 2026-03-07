package org.example.shared;

import java.util.function.Function;

public sealed interface Result<T> permits Success, Failure {
    boolean isSuccess();
    T getOrElse(T defaultValue);
    <U> Result<U> map(Function<T, U> fn);
}