package com.example.service;

import com.example.model.Reservation;
import com.example.model.Salle;
import com.example.repository.SalleRepository;
import com.example.repository.SalleRepositoryImpl;
import com.example.util.PaginationResult;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation du service.
 *
 * <p>En conteneur JEE, {@code @Inject} fournit le repository et
 * {@code @PersistenceContext} fournit l'{@link EntityManager}. Hors conteneur,
 * l'application de demonstration appelle {@link #setEntityManager(EntityManager)} :
 * l'EM est alors propage au repository, ce qui evite toute dependance a un
 * serveur d'applications.</p>
 *
 * <p>Le cycle de vie de l'EntityManager reste a la charge de l'appelant (comme
 * dans l'enonce, ou {@code App} ferme l'EM puis l'EMF) : le service ne fait
 * qu'ouvrir et valider les transactions dont il a besoin.</p>
 */
@Stateless
public class SalleServiceImpl implements SalleService {

    @Inject
    private SalleRepository salleRepository;

    /**
     * Injecte l'EntityManager dans ce service et, en cascade, dans le
     * repository. Utilise uniquement hors conteneur JEE.
     */
    public void setEntityManager(EntityManager entityManager) {
        if (salleRepository == null) {
            salleRepository = new SalleRepositoryImpl();
        }
        if (salleRepository instanceof SalleRepositoryImpl) {
            ((SalleRepositoryImpl) salleRepository).setEntityManager(entityManager);
        }
    }

    public void setSalleRepository(SalleRepository salleRepository) {
        this.salleRepository = salleRepository;
    }

    public SalleRepository getSalleRepository() {
        return salleRepository;
    }

    @Override
    public List<Salle> getAvailableRooms(LocalDateTime debut, LocalDateTime fin) {
        // Des regles metier pourraient s'appliquer ici avant l'appel au repository.
        return salleRepository.findAvailableRooms(debut, fin);
    }

    @Override
    public List<Salle> searchRooms(String nom, Integer capaciteMin,
                                   LocalDateTime debut, LocalDateTime fin,
                                   int page, int size) {
        return salleRepository.searchRooms(nom, capaciteMin, debut, fin, page, size);
    }

    @Override
    public long countRooms(String nom, Integer capaciteMin,
                           LocalDateTime debut, LocalDateTime fin) {
        return salleRepository.countRooms(nom, capaciteMin, debut, fin);
    }

    /**
     * Assemble une page de resultats avec ses metadonnees : le comptage utilise
     * exactement les memes criteres que la selection.
     */
    public PaginationResult<Salle> rechercherSallesPaginees(String nom, Integer capaciteMin,
                                                            LocalDateTime debut, LocalDateTime fin,
                                                            int page, int size) {
        List<Salle> items = searchRooms(nom, capaciteMin, debut, fin, page, size);
        long total = countRooms(nom, capaciteMin, debut, fin);
        return new PaginationResult<>(items, page, size, total);
    }

    @Override
    public Long creerSalle(String nom, int capacite) {
        EntityManager em = entityManager();
        boolean proprio = !em.getTransaction().isActive();
        if (proprio) {
            em.getTransaction().begin();
        }
        try {
            Salle salle = new Salle(nom, capacite);
            em.persist(salle);
            if (proprio) {
                em.getTransaction().commit();
            }
            return salle.getId();
        } catch (RuntimeException e) {
            if (proprio && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        }
    }

    @Override
    public Long reserver(Long salleId, LocalDateTime debut, LocalDateTime fin) {
        EntityManager em = entityManager();
        boolean proprio = !em.getTransaction().isActive();
        if (proprio) {
            em.getTransaction().begin();
        }
        try {
            Salle salle = em.find(Salle.class, salleId);
            if (salle == null) {
                throw new IllegalArgumentException("Salle inconnue : " + salleId);
            }
            Reservation reservation = new Reservation(debut, fin);
            // addReservation maintient les deux cotes de l'association : sans
            // cela, la cle etrangere salle_id resterait nulle.
            salle.addReservation(reservation);
            em.persist(reservation);
            if (proprio) {
                em.getTransaction().commit();
            }
            return reservation.getId();
        } catch (RuntimeException e) {
            if (proprio && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        }
    }

    private EntityManager entityManager() {
        if (!(salleRepository instanceof SalleRepositoryImpl)) {
            throw new IllegalStateException("Aucun EntityManager disponible : appelez setEntityManager(...)");
        }
        EntityManager em = ((SalleRepositoryImpl) salleRepository).getEntityManager();
        if (em == null) {
            throw new IllegalStateException("Aucun EntityManager disponible : appelez setEntityManager(...)");
        }
        return em;
    }
}
