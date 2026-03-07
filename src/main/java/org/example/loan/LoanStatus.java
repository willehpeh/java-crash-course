package org.example.loan;

public enum LoanStatus {
    ACTIVE {
        @Override
        public LoanStatus transitionTo(LoanStatus target) {
            if (target == ACTIVE) {
                throw new IllegalStateException("Cannot transition from ACTIVE to " + target);
            }
            return target;
        }
    },
    RETURNED {
        @Override
        public LoanStatus transitionTo(LoanStatus target) {
            if (target == RETURNED || target == OVERDUE) {
                throw new IllegalStateException("Cannot transition from RETURNED to " + target);
            }
            return target;
        }
    },
    OVERDUE {
        @Override
        public LoanStatus transitionTo(LoanStatus target) {
            if (target == OVERDUE) {
                throw new IllegalStateException("Cannot transition from OVERDUE to " + target);
            }
            return target;
        }
    };

    public abstract LoanStatus transitionTo(LoanStatus target);
}
