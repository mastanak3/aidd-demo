package com.example.library.domain;

import com.example.library.domain.model.MemberType;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class LendingPolicy {

    private static final int GENERAL_MAX_LOANS = 3;
    private static final int PREMIUM_MAX_LOANS = 10;
    private static final int GENERAL_LOAN_PERIOD_DAYS = 14;
    private static final int PREMIUM_LOAN_PERIOD_DAYS = 30;
    private static final int GENERAL_DAILY_RATE = 50;
    private static final int PREMIUM_DAILY_RATE = 20;
    private static final int GENERAL_MAX_FEE = 1000;
    private static final int PREMIUM_MAX_FEE = 500;

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

    public static int calculateOverdueFee(MemberType memberType, LocalDate dueDate, LocalDate returnDate) {
        long overdueDays = ChronoUnit.DAYS.between(dueDate, returnDate);
        int dailyRate = switch (memberType) {
            case GENERAL -> GENERAL_DAILY_RATE;
            case PREMIUM -> PREMIUM_DAILY_RATE;
        };
        int maxFee = switch (memberType) {
            case GENERAL -> GENERAL_MAX_FEE;
            case PREMIUM -> PREMIUM_MAX_FEE;
        };
        int fee = (int) overdueDays * dailyRate;
        return Math.min(fee, maxFee);
    }
}
