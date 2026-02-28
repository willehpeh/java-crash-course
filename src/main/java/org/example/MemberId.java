package org.example;

public record MemberId(String value) {
    public MemberId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("MemberId cannot be null");
        }
    }
}
