package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryLoanRepository implements LoanRepository {

    private final Map<String, List<String>> loans = new HashMap<>();

    public void save(String memberId, String bookId) {
        loans.computeIfAbsent(memberId, _ -> new ArrayList<>()).add(bookId);
    }

    public void delete(String memberId, String bookId) {
        loans.getOrDefault(memberId, List.of()).remove(bookId);
    }

    public List<String> findBooksByMember(String memberId) {
        return loans.getOrDefault(memberId, List.of());
    }

    public boolean isBookOnLoan(String bookId) {
        return loans.values().stream().anyMatch(books -> books.contains(bookId));
    }
}
