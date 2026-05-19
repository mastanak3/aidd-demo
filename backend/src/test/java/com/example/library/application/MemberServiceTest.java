package com.example.library.application;

import com.example.library.TestDatabaseCleaner;
import com.example.library.domain.model.Member;
import com.example.library.domain.model.MemberType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MemberServiceTest {

    @Autowired
    MemberService memberService;

    @Autowired
    TestDatabaseCleaner dbCleaner;

    @BeforeEach
    void setUp() {
        dbCleaner.cleanAll();
    }

    @Test
    void 一般会員を登録できる() {
        Member member = memberService.create("田中太郎", "tanaka@example.com", MemberType.GENERAL);

        assertNotNull(member.getId());
        assertEquals("田中太郎", member.getName());
        assertEquals("tanaka@example.com", member.getEmail());
        assertEquals(MemberType.GENERAL, member.getMemberType());
    }

    @Test
    void プレミアム会員を登録できる() {
        Member member = memberService.create("鈴木花子", "suzuki@example.com", MemberType.PREMIUM);

        assertEquals(MemberType.PREMIUM, member.getMemberType());
    }

    @Test
    void 会員をIDで検索できる() {
        Member created = memberService.create("佐藤次郎", "sato@example.com", MemberType.GENERAL);

        Member found = memberService.findById(created.getId());

        assertEquals(created.getId(), found.getId());
        assertEquals("佐藤次郎", found.getName());
    }

    @Test
    void 全会員を取得できる() {
        memberService.create("会員A", "a@example.com", MemberType.GENERAL);
        memberService.create("会員B", "b@example.com", MemberType.PREMIUM);

        var members = memberService.findAll();

        assertTrue(members.size() >= 2);
    }

    @Test
    void 会員を更新できる() {
        Member created = memberService.create("旧名前", "old@example.com", MemberType.GENERAL);

        Member updated = memberService.update(created.getId(), "新名前", "new@example.com", MemberType.PREMIUM);

        assertEquals("新名前", updated.getName());
        assertEquals("new@example.com", updated.getEmail());
        assertEquals(MemberType.PREMIUM, updated.getMemberType());
    }

    @Test
    void 会員を削除できる() {
        Member created = memberService.create("削除対象", "del@example.com", MemberType.GENERAL);
        String id = created.getId();

        memberService.delete(id);

        assertThrows(IllegalArgumentException.class, () -> memberService.findById(id));
    }

    @Test
    void 存在しないIDで検索すると例外() {
        assertThrows(IllegalArgumentException.class, () -> memberService.findById("9999999"));
    }
}
