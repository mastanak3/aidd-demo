package com.example.library;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@Interceptor
@Transactional
@Priority(Interceptor.Priority.APPLICATION)
public class TestTransactionInterceptor {

    @Inject
    private EntityManager em;

    @AroundInvoke
    public Object manageTransaction(InvocationContext ctx) throws Exception {
        boolean started = false;
        if (!em.getTransaction().isActive()) {
            em.getTransaction().begin();
            started = true;
        }
        try {
            Object result = ctx.proceed();
            if (started && em.getTransaction().isActive()) {
                em.getTransaction().commit();
            }
            return result;
        } catch (Exception e) {
            if (started && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        }
    }
}
