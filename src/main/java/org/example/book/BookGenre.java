package org.example.book;

public enum BookGenre {
    FICTION("Fiction", 30),
    NON_FICTION("Non-fiction", 180),
    REFERENCE("Reference", 7);

    private final String displayName;
    private final int maxLoanDays;

    BookGenre(String displayName, int maxLoanDays) {
        this.displayName = displayName;
        this.maxLoanDays = maxLoanDays;
    }

    public String displayName() { return displayName; }
    public int maxLoanDays() { return maxLoanDays; }
}
