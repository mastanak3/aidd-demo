package com.example.library.infrastructure.database;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class EntityManagerProducer {

    @PersistenceContext(unitName = "library")
    private EntityManager em;

    @Produces
    public EntityManager getEntityManager() {
        return em;
    }
}
