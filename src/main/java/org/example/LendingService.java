package org.example;

import java.util.List;

public class LendingService {

    private final LoanRepository loanRepository;
    private final int lendingLimit;

    public LendingService(LoanRepository loanRepository, LendingServiceConfig config) {
        this.loanRepository = loanRepository;
        lendingLimit = config.lendingLimit();
    }

    public void borrowBook(String member, String book) {
        if (loanRepository.isBookOnLoan(book)) {
            throw new IllegalStateException("Book is already on loan");
        }
        if (loansFor(member).size() >= lendingLimit) {
            throw new IllegalStateException("Member has reached borrowing limit");
        }
        loanRepository.save(member, book);
    }

    public List<String> loansFor(String member) {
        return loanRepository.findBooksByMember(member);
    }

    public void returnBook(String member, String book) {
        loanRepository.delete(member, book);
    }
}
