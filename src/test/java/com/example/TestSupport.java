package com.example;

import com.example.model.Salle;
import com.example.repository.SalleRepository;
import com.example.repository.SalleRepositoryImpl;
import com.example.service.SalleServiceImpl;
import com.example.util.JpaUtil;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Support commun aux tests : acces a l'EntityManagerFactory du PU
 * {@code SallePU}, remise a zero de la base et jeu de donnees partage.
 *
 * <p>Le PU utilise {@code hbm2ddl.auto=update} et une base H2 en memoire
 * conservee ({@code DB_CLOSE_DELAY=-1}) : le schema et les donnees survivent a
 * la fermeture de l'EMF. Chaque test doit donc nettoyer explicitement les
 * tables, en supprimant d'abord {@code reservations} (porteuse de la cle
 * etrangere) puis {@code salles}.</p>
 */
public final class TestSupport {

    /** Creneau de reference : 1er juin 2024, 09h00 - 11h00. */
    public static final LocalDateTime CRENEAU_DEBUT = LocalDateTime.of(2024, 6, 1, 9, 0);
    public static final LocalDateTime CRENEAU_FIN = LocalDateTime.of(2024, 6, 1, 11, 0);

    private TestSupport() {
    }

    public static EntityManagerFactory entityManagerFactory() {
        return JpaUtil.getEntityManagerFactory();
    }

    public static EntityManager openEntityManager() {
        return entityManagerFactory().createEntityManager();
    }

    /**
     * Service pret a l'emploi, relie a un EntityManager neuf. L'appelant est
     * responsable de la fermeture de l'EM retourne.
     */
    public static SalleServiceImpl newService(EntityManager em) {
        SalleServiceImpl service = new SalleServiceImpl();
        service.setEntityManager(em);
        return service;
    }

    public static SalleRepository newRepository(EntityManager em) {
        SalleRepositoryImpl repository = new SalleRepositoryImpl();
        repository.setEntityManager(em);
        return repository;
    }

    /** Vide les deux tables dans l'ordre compatible avec la cle etrangere. */
    public static void cleanDatabase(EntityManager em) {
        em.getTransaction().begin();
        try {
            em.createQuery("DELETE FROM Reservation").executeUpdate();
            em.createQuery("DELETE FROM Salle").executeUpdate();
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        }
        em.clear();
    }

    public static long count(EntityManager em, Class<?> entity) {
        return em.createQuery("SELECT COUNT(e) FROM " + entity.getSimpleName() + " e", Long.class)
                 .getSingleResult();
    }

    public static List<Salle> allSalles(EntityManager em) {
        return em.createQuery("SELECT s FROM Salle s ORDER BY s.nom", Salle.class).getResultList();
    }

    /**
     * Jeu de donnees de reference :
     * <ul>
     *   <li>Amphi A (120) : occupe 09h-11h -> indisponible</li>
     *   <li>Salle B12 (30) : occupe 10h-12h -> indisponible (chevauchement partiel)</li>
     *   <li>Salle C3 (24) : occupe 08h-09h -> disponible (creneau jointif avant)</li>
     *   <li>Salle D1 (45) : occupe 11h-13h -> disponible (creneau jointif apres)</li>
     *   <li>Salle E7 (60) : libre -> disponible</li>
     *   <li>Amphi B (200) : libre -> disponible</li>
     * </ul>
     */
    public static void seedStandardDataset(SalleServiceImpl service) {
        Long amphiA = service.creerSalle("Amphi A", 120);
        Long salleB12 = service.creerSalle("Salle B12", 30);
        Long salleC3 = service.creerSalle("Salle C3", 24);
        Long salleD1 = service.creerSalle("Salle D1", 45);
        service.creerSalle("Salle E7", 60);
        service.creerSalle("Amphi B", 200);

        service.reserver(amphiA, LocalDateTime.of(2024, 6, 1, 9, 0), LocalDateTime.of(2024, 6, 1, 11, 0));
        service.reserver(salleB12, LocalDateTime.of(2024, 6, 1, 10, 0), LocalDateTime.of(2024, 6, 1, 12, 0));
        service.reserver(salleC3, LocalDateTime.of(2024, 6, 1, 8, 0), LocalDateTime.of(2024, 6, 1, 9, 0));
        service.reserver(salleD1, LocalDateTime.of(2024, 6, 1, 11, 0), LocalDateTime.of(2024, 6, 1, 13, 0));
    }

    /** Noms des salles disponibles sur le creneau de reference, tries. */
    public static List<String> nomsTries(List<Salle> salles) {
        return salles.stream().map(Salle::getNom).sorted().collect(java.util.stream.Collectors.toList());
    }
}
