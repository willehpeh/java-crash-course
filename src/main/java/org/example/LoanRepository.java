package org.example;

import java.util.List;

public interface LoanRepository {
    void save(String memberId, String bookId);

    void delete(String memberId, String bookId);

    List<String> findBooksByMember(String memberId);

    boolean isBookOnLoan(String bookId);
}
