package com.example.library;

import com.example.library.application.BookService;
import com.example.library.application.LoanService;
import com.example.library.application.MemberService;
import com.example.library.resource.*;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

import java.net.URI;

public class App {

    public static final String BASE_URI = "http://localhost:8080/api/";

    public static HttpServer startServer(WeldContainer container, int port) {
        BookService bookService = container.select(BookService.class).get();
        MemberService memberService = container.select(MemberService.class).get();
        LoanService loanService = container.select(LoanService.class).get();

        ResourceConfig config = new ResourceConfig();
        config.register(new BookResource(bookService));
        config.register(new MemberResource(memberService));
        config.register(new LoanResource(loanService));
        config.register(JacksonFeature.class);
        config.register(new JacksonConfig());
        config.register(new ExceptionMappers.IllegalArgumentExceptionMapper());
        config.register(new ExceptionMappers.IllegalStateExceptionMapper());

        String uri = "http://localhost:" + port + "/api/";
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(uri), config);
    }

    public static HttpServer startServer(WeldContainer container) {
        return startServer(container, 8080);
    }

    public static void main(String[] args) throws Exception {
        Weld weld = new Weld();
        WeldContainer container = weld.initialize();

        HttpServer server = startServer(container);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdownNow();
            weld.shutdown();
        }));

        System.out.println("Library API started at " + BASE_URI);
        System.out.println("Press Enter to stop...");

        if (System.in.read() == -1) {
            Thread.currentThread().join();
        }

        server.shutdown();
        weld.shutdown();
    }
}
