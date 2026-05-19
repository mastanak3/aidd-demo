package com.example.library.application;

import com.example.library.domain.model.Member;
import com.example.library.domain.model.MemberType;
import com.example.library.domain.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member create(String id, String name, String email, MemberType memberType) {
        Member member = new Member(id, name, email, memberType);
        return memberRepository.save(member);
    }

    public Member create(String name, String email, MemberType memberType) {
        String id = generateMemberId();
        return create(id, name, email, memberType);
    }

    private String generateMemberId() {
        List<Member> allMembers = memberRepository.findAll();
        if (allMembers.isEmpty()) {
            return "0000001";
        }
        String maxId = allMembers.stream()
                .map(Member::getId)
                .max(String::compareTo)
                .orElse("0000000");
        long nextId = Long.parseLong(maxId) + 1;
        return String.format("%07d", nextId);
    }

    @Transactional(readOnly = true)
    public Member findById(String id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("会員が見つかりません: ID=" + id));
    }

    @Transactional(readOnly = true)
    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public Member update(String id, String name, String email, MemberType memberType) {
        Member member = findById(id);
        member.setName(name);
        member.setEmail(email);
        member.setMemberType(memberType);
        return memberRepository.save(member);
    }

    public void delete(String id) {
        findById(id);
        memberRepository.deleteById(id);
    }
}
