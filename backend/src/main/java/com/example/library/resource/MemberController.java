package com.example.library.resource;

import com.example.library.application.MemberService;
import com.example.library.domain.model.Member;
import com.example.library.domain.model.MemberType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public List<Member> findAll() {
        return memberService.findAll();
    }

    @GetMapping("/{id}")
    public Member findById(@PathVariable String id) {
        return memberService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Member> create(@RequestBody MemberRequest request) {
        Member member = memberService.create(request.id(), request.name(), request.email(), request.memberType());
        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }

    @PutMapping("/{id}")
    public Member update(@PathVariable String id, @RequestBody MemberRequest request) {
        return memberService.update(id, request.name(), request.email(), request.memberType());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        memberService.delete(id);
        return ResponseEntity.noContent().build();
    }

    public record MemberRequest(String id, String name, String email, MemberType memberType) {
    }
}
