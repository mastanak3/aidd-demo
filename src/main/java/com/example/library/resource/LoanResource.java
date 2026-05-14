package com.example.library.resource;

import com.example.library.application.LoanService;
import com.example.library.domain.model.Loan;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/loans")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LoanResource {

    private final LoanService loanService;

    public LoanResource(LoanService loanService) {
        this.loanService = loanService;
    }

    @GET
    public List<Loan> findAll() {
        return loanService.findAll();
    }

    @GET
    @Path("/{id}")
    public Loan findById(@PathParam("id") Long id) {
        return loanService.findById(id);
    }

    @POST
    public Response borrowBook(LoanRequest request) {
        Loan loan = loanService.borrowBook(request.memberId(), request.bookId());
        return Response.status(Response.Status.CREATED).entity(loan).build();
    }

    @POST
    @Path("/{id}/return")
    public Loan returnBook(@PathParam("id") Long id) {
        return loanService.returnBook(id);
    }

    public record LoanRequest(Long memberId, Long bookId) {
    }
}
