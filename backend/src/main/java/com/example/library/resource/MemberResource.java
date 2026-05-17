package com.example.library.resource;

import com.example.library.application.MemberService;
import com.example.library.domain.model.Member;
import com.example.library.domain.model.MemberType;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/members")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class MemberResource {

    @Inject
    private MemberService memberService;

    public MemberResource() {
    }

    public MemberResource(MemberService memberService) {
        this.memberService = memberService;
    }

    @GET
    public List<Member> findAll() {
        return memberService.findAll();
    }

    @GET
    @Path("/{id}")
    public Member findById(@PathParam("id") Long id) {
        return memberService.findById(id);
    }

    @POST
    public Response create(MemberRequest request) {
        Member member = memberService.create(request.name(), request.email(), request.memberType());
        return Response.status(Response.Status.CREATED).entity(member).build();
    }

    @PUT
    @Path("/{id}")
    public Member update(@PathParam("id") Long id, MemberRequest request) {
        return memberService.update(id, request.name(), request.email(), request.memberType());
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        memberService.delete(id);
        return Response.noContent().build();
    }

    public record MemberRequest(String name, String email, MemberType memberType) {
    }
}
