package com.example.library.domain.model;

public class Member {

    private Long id;
    private String name;
    private String email;
    private MemberType memberType;

    public Member() {
    }

    public Member(String name, String email, MemberType memberType) {
        this.name = name;
        this.email = email;
        this.memberType = memberType;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public MemberType getMemberType() {
        return memberType;
    }

    public void setMemberType(MemberType memberType) {
        this.memberType = memberType;
    }
}
