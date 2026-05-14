package com.example.library.domain.repository;

import com.example.library.domain.model.Member;

import java.util.List;
import java.util.Optional;

public interface MemberRepository {

    Member save(Member member);

    Optional<Member> findById(Long id);

    List<Member> findAll();

    void deleteById(Long id);
}
