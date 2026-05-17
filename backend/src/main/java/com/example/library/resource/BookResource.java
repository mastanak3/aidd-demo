package com.example.library.resource;

import com.example.library.application.BookService;
import com.example.library.domain.model.Book;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/books")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class BookResource {

    @Inject
    private BookService bookService;

    public BookResource() {
    }

    public BookResource(BookService bookService) {
        this.bookService = bookService;
    }

    @GET
    public List<Book> findAll() {
        return bookService.findAll();
    }

    @GET
    @Path("/{id}")
    public Book findById(@PathParam("id") Long id) {
        return bookService.findById(id);
    }

    @POST
    public Response create(BookRequest request) {
        Book book = bookService.create(request.title(), request.author(), request.isbn());
        return Response.status(Response.Status.CREATED).entity(book).build();
    }

    @PUT
    @Path("/{id}")
    public Book update(@PathParam("id") Long id, BookRequest request) {
        return bookService.update(id, request.title(), request.author(), request.isbn());
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        bookService.delete(id);
        return Response.noContent().build();
    }

    public record BookRequest(String title, String author, String isbn) {
    }
}
