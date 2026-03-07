package org.example.shared;

public interface Searchable {
    String searchableText();
    default boolean matches(String query) {
        return searchableText().toLowerCase().contains(query.toLowerCase());
    }
}
