package com.example.library.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MemberTest {

    @Test
    void 一般会員を生成できる() {
        Member member = new Member("0000001", "田中太郎", "tanaka@example.com", MemberType.GENERAL);

        assertEquals("0000001", member.getId());
        assertEquals("田中太郎", member.getName());
        assertEquals("tanaka@example.com", member.getEmail());
        assertEquals(MemberType.GENERAL, member.getMemberType());
    }

    @Test
    void プレミアム会員を生成できる() {
        Member member = new Member("0000002", "鈴木花子", "suzuki@example.com", MemberType.PREMIUM);

        assertEquals("0000002", member.getId());
        assertEquals("鈴木花子", member.getName());
        assertEquals("suzuki@example.com", member.getEmail());
        assertEquals(MemberType.PREMIUM, member.getMemberType());
    }
}
