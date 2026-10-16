package com.example.util;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * Point d'acces unique a l'{@link EntityManagerFactory} de l'application.
 *
 * <p>L'application de demonstration est une application Java SE : elle ne
 * dispose pas d'un conteneur JEE capable de fournir un {@link EntityManager}.
 * Cette classe fabrique donc l'EMF une seule fois (creation couteuse) et le
 * reutilise, puis expose {@link #close()} pour la fermeture en fin
 * d'execution.</p>
 */
public final class JpaUtil {

    private static final String PERSISTENCE_UNIT = "SallePU";

    private static EntityManagerFactory entityManagerFactory;

    private JpaUtil() {
    }

    /**
     * Retourne l'EMF, en le creant au premier appel. La methode est
     * synchronisee pour rester sure en environnement multi-thread (tests
     * executes en parallele, par exemple).
     */
    public static synchronized EntityManagerFactory getEntityManagerFactory() {
        if (entityManagerFactory == null || !entityManagerFactory.isOpen()) {
            entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
        }
        return entityManagerFactory;
    }

    public static EntityManager getEntityManager() {
        return getEntityManagerFactory().createEntityManager();
    }

    public static synchronized void close() {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
        }
        entityManagerFactory = null;
    }
}
