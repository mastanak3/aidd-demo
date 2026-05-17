package com.example.library;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class TestDatabaseCleaner {

    @Inject
    private EntityManager em;

    public void cleanAll() {
        em.getTransaction().begin();
        em.createNativeQuery("SET session_replication_role = 'replica'").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE loans").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE books RESTART IDENTITY CASCADE").executeUpdate();
        em.createNativeQuery("TRUNCATE TABLE members RESTART IDENTITY CASCADE").executeUpdate();
        em.createNativeQuery("SET session_replication_role = 'origin'").executeUpdate();
        em.getTransaction().commit();
        em.clear();
    }
}
