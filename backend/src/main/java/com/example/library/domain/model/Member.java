package com.example.library.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "members")
public class Member {

    @Id
    @Column(length = 7)
    private String id;
    private String name;
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_type")
    private MemberType memberType;

    public Member() {
    }

    public Member(String id, String name, String email, MemberType memberType) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.memberType = memberType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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
