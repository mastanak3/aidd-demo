package com.example.library.application;

import com.example.library.domain.model.Member;
import com.example.library.domain.model.MemberType;
import com.example.library.domain.repository.MemberRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class MemberService {

    @Inject
    private MemberRepository memberRepository;

    public Member create(String name, String email, MemberType memberType) {
        Member member = new Member(name, email, memberType);
        return memberRepository.save(member);
    }

    public Member findById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("会員が見つかりません: ID=" + id));
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public Member update(Long id, String name, String email, MemberType memberType) {
        Member member = findById(id);
        member.setName(name);
        member.setEmail(email);
        member.setMemberType(memberType);
        return memberRepository.save(member);
    }

    public void delete(Long id) {
        findById(id);
        memberRepository.deleteById(id);
    }
}
