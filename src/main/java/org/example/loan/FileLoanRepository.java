package org.example.loan;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.book.BookId;
import org.example.lending.MemberId;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FileLoanRepository implements LoanRepository {

    private final Path path;
    private final ObjectMapper objectMapper;

    public FileLoanRepository(Path path, ObjectMapper objectMapper) {
        this.path = path;
        this.objectMapper = objectMapper;
        createLoanFileIfDoesNotExist(path);
    }

    private void createLoanFileIfDoesNotExist(Path path) {
        if (!Files.exists(path)) {
            try {
                Files.writeString(path, "{}");
            } catch (IOException e) {
                throw new LoanPersistenceException("Failed to create loan file", e);
            }
        }
    }

    private Map<String, List<String>> loans() {
        try {
            String json = Files.readString(path);
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new LoanPersistenceException("Failed to save loan", e);
        }
    }

    private void saveLoans(Map<String, List<String>> map) {
        try {
            Files.writeString(path, objectMapper.writeValueAsString(map));
        } catch (IOException e) {
            throw new LoanPersistenceException("Failed to save loan", e);
        }
    }

    @Override
    public void save(MemberId memberId, BookId bookId) {
        Map<String, List<String>> map = loans();
        map.computeIfAbsent(memberId.value(), _ -> new ArrayList<>()).add(bookId.value());
        saveLoans(map);
    }

    @Override
    public void delete(MemberId memberId, BookId bookId) {
        Map<String, List<String>> map = loans();
        map.getOrDefault(memberId.value(), List.of()).remove(bookId.value());
        saveLoans(map);
    }

    @Override
    public List<BookId> findBooksByMember(MemberId memberId) {
        return loans().getOrDefault(memberId.value(), List.of()).stream()
                .map(BookId::new)
                .toList();
    }

    @Override
    public boolean isBookOnLoan(BookId bookId) {
        return loans().values().stream()
                .anyMatch(books -> books.contains(bookId.value()));
    }

    @Override
    public Optional<MemberId> borrowerOfBook(BookId bookId) {
        return loans().entrySet().stream()
                .filter(entry -> entry.getValue().contains(bookId.value()))
                .map(Map.Entry::getKey)
                .map(MemberId::new)
                .findFirst();
    }

    @Override
    public List<BookId> allBooksOnLoan() {
        return loans().values().stream()
                .flatMap(List::stream)
                .map(BookId::new)
                .toList();
    }
}
