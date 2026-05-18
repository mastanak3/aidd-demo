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

    public Member create(String name, String email, MemberType memberType) {
        Member member = new Member(name, email, memberType);
        return memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public Member findById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("会員が見つかりません: ID=" + id));
    }

    @Transactional(readOnly = true)
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
