package org.example;

public enum LoanStatus {
    ACTIVE {
        @Override
        public boolean canTransitionTo(LoanStatus status) {
            return status == OVERDUE || status == RETURNED;
        }
    },
    RETURNED {
        @Override
        public boolean canTransitionTo(LoanStatus status) {
            return status == ACTIVE;
        }
    },
    OVERDUE {
        @Override
        public boolean canTransitionTo(LoanStatus status) {
            return status == ACTIVE || status == RETURNED;
        }
    };

    public abstract boolean canTransitionTo(LoanStatus status);
}
