package com.example.library.domain;

import com.example.library.domain.model.MemberType;

import java.time.LocalDate;

public class LendingPolicy {

    private static final int GENERAL_MAX_LOANS = 3;
    private static final int PREMIUM_MAX_LOANS = 10;
    private static final int GENERAL_LOAN_PERIOD_DAYS = 14;
    private static final int PREMIUM_LOAN_PERIOD_DAYS = 30;

    public static int getMaxLoans(MemberType memberType) {
        return switch (memberType) {
            case GENERAL -> GENERAL_MAX_LOANS;
            case PREMIUM -> PREMIUM_MAX_LOANS;
        };
    }

    public static int getLoanPeriodDays(MemberType memberType) {
        return switch (memberType) {
            case GENERAL -> GENERAL_LOAN_PERIOD_DAYS;
            case PREMIUM -> PREMIUM_LOAN_PERIOD_DAYS;
        };
    }

    public static LocalDate calculateDueDate(MemberType memberType, LocalDate loanDate) {
        return loanDate.plusDays(getLoanPeriodDays(memberType));
    }

    public static boolean canBorrow(MemberType memberType, int currentLoanCount) {
        return currentLoanCount < getMaxLoans(memberType);
    }
}
