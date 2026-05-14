package com.example.library.domain;

import com.example.library.domain.model.MemberType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class LendingPolicyTest {

    @Test
    void 一般会員の貸出上限は3冊() {
        assertEquals(3, LendingPolicy.getMaxLoans(MemberType.GENERAL));
    }

    @Test
    void プレミアム会員の貸出上限は10冊() {
        assertEquals(10, LendingPolicy.getMaxLoans(MemberType.PREMIUM));
    }

    @Test
    void 一般会員の貸出期間は14日() {
        assertEquals(14, LendingPolicy.getLoanPeriodDays(MemberType.GENERAL));
    }

    @Test
    void プレミアム会員の貸出期間は30日() {
        assertEquals(30, LendingPolicy.getLoanPeriodDays(MemberType.PREMIUM));
    }

    @Test
    void 一般会員の返却期限は貸出日から14日後() {
        LocalDate loanDate = LocalDate.of(2025, 5, 1);
        LocalDate dueDate = LendingPolicy.calculateDueDate(MemberType.GENERAL, loanDate);
        assertEquals(LocalDate.of(2025, 5, 15), dueDate);
    }

    @Test
    void プレミアム会員の返却期限は貸出日から30日後() {
        LocalDate loanDate = LocalDate.of(2025, 5, 1);
        LocalDate dueDate = LendingPolicy.calculateDueDate(MemberType.PREMIUM, loanDate);
        assertEquals(LocalDate.of(2025, 5, 31), dueDate);
    }

    @Test
    void 一般会員は貸出2冊なら追加で借りられる() {
        assertTrue(LendingPolicy.canBorrow(MemberType.GENERAL, 2));
    }

    @Test
    void 一般会員は貸出3冊なら追加で借りられない() {
        assertFalse(LendingPolicy.canBorrow(MemberType.GENERAL, 3));
    }

    @Test
    void プレミアム会員は貸出9冊なら追加で借りられる() {
        assertTrue(LendingPolicy.canBorrow(MemberType.PREMIUM, 9));
    }

    @Test
    void プレミアム会員は貸出10冊なら追加で借りられない() {
        assertFalse(LendingPolicy.canBorrow(MemberType.PREMIUM, 10));
    }

    @Test
    void 貸出0冊なら借りられる() {
        assertTrue(LendingPolicy.canBorrow(MemberType.GENERAL, 0));
        assertTrue(LendingPolicy.canBorrow(MemberType.PREMIUM, 0));
    }
}
