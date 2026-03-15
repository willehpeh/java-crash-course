package org.example.loan;

public class InMemoryLoanRepositoryTest extends LoanRepositoryContractTest {
    @Override
    protected LoanRepository createRepository() {
        return new InMemoryLoanRepository();
    }
}
