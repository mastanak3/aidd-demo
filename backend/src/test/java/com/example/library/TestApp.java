package com.example.library;

import com.example.library.application.BookService;
import com.example.library.application.LoanService;
import com.example.library.application.MemberService;
import com.example.library.resource.*;
import io.undertow.Undertow;
import io.undertow.servlet.api.DeploymentInfo;
import org.jboss.resteasy.core.ResteasyDeploymentImpl;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.weld.environment.se.WeldContainer;

import java.util.ArrayList;
import java.util.List;

public class TestApp {

    public static UndertowJaxrsServer startServer(WeldContainer container, int port) {
        BookService bookService = container.select(BookService.class).get();
        MemberService memberService = container.select(MemberService.class).get();
        LoanService loanService = container.select(LoanService.class).get();

        ResteasyDeployment deployment = new ResteasyDeploymentImpl();

        List<Object> resources = new ArrayList<>();
        resources.add(new BookResource(bookService));
        resources.add(new MemberResource(memberService));
        resources.add(new LoanResource(loanService));
        deployment.setResources(resources);

        List<Object> providers = new ArrayList<>();
        providers.add(new JacksonConfig());
        providers.add(new ExceptionMappers.IllegalArgumentExceptionMapper());
        providers.add(new ExceptionMappers.IllegalStateExceptionMapper());
        deployment.setProviders(providers);

        UndertowJaxrsServer server = new UndertowJaxrsServer();
        server.start(Undertow.builder().addHttpListener(port, "localhost"));

        DeploymentInfo di = server.undertowDeployment(deployment, "/");
        di.setClassLoader(TestApp.class.getClassLoader());
        di.setContextPath("/api");
        di.setDeploymentName("test-" + port);

        server.deploy(di);
        return server;
    }
}
