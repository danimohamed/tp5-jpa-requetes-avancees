package com.example.repository;

import com.example.model.Reservation;
import com.example.model.Salle;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation du repository.
 *
 * <p>Dans un conteneur JEE, {@code @Stateless} et {@code @PersistenceContext}
 * suffisent a injecter l'{@link EntityManager}. Hors conteneur (application
 * Java SE, tests unitaires), l'injection est faite manuellement via
 * {@link #setEntityManager(EntityManager)} : le comportement des requetes est
 * strictement identique.</p>
 */
@Stateless
public class SalleRepositoryImpl implements SalleRepository {

    @PersistenceContext(unitName = "SallePU")
    private EntityManager entityManager;

    /** Injection manuelle, utilisee hors conteneur JEE. */
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    /**
     * Requete JPQL de l'enonce : la sous-requete selectionne les salles dont une
     * reservation intersecte le creneau recherche, la requete principale les
     * exclut par {@code NOT IN}.
     */
    @Override
    public List<Salle> findAvailableRooms(LocalDateTime debut, LocalDateTime fin) {
        String jpql = "SELECT s FROM Salle s WHERE s.id NOT IN ("
                + "SELECT r.salle.id FROM Reservation r WHERE "
                + "(:debut < r.fin AND :fin > r.debut)"
                + ")";

        TypedQuery<Salle> query = entityManager.createQuery(jpql, Salle.class);
        query.setParameter("debut", debut);
        query.setParameter("fin", fin);

        return query.getResultList();
    }

    @Override
    public List<Salle> searchRooms(String nom, Integer capaciteMin,
                                   LocalDateTime debut, LocalDateTime fin,
                                   int page, int size) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Salle> cq = cb.createQuery(Salle.class);
        Root<Salle> salle = cq.from(Salle.class);

        List<Predicate> predicates = buildPredicates(cb, cq, salle, nom, capaciteMin, debut, fin);

        cq.select(salle)
          .where(cb.and(predicates.toArray(new Predicate[0])))
          .orderBy(cb.asc(salle.get("nom")));

        TypedQuery<Salle> query = entityManager.createQuery(cq);
        query.setFirstResult(Math.max(0, (page - 1) * size));
        query.setMaxResults(size);

        return query.getResultList();
    }

    @Override
    public long countRooms(String nom, Integer capaciteMin,
                           LocalDateTime debut, LocalDateTime fin) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Salle> countRoot = countQuery.from(Salle.class);

        // Les memes predicats sont appliques : sinon le nombre total de pages
        // serait incoherent avec le contenu des pages.
        List<Predicate> predicates = buildPredicates(cb, countQuery, countRoot, nom, capaciteMin, debut, fin);

        countQuery.select(cb.count(countRoot))
                  .where(cb.and(predicates.toArray(new Predicate[0])));

        return entityManager.createQuery(countQuery).getSingleResult();
    }

    /**
     * Construit la liste des predicats correspondant aux criteres renseignes.
     * Utilise a la fois par la requete de selection et par la requete de
     * comptage.
     */
    private List<Predicate> buildPredicates(CriteriaBuilder cb, CriteriaQuery<?> cq, Root<Salle> salle,
                                            String nom, Integer capaciteMin,
                                            LocalDateTime debut, LocalDateTime fin) {
        List<Predicate> predicates = new ArrayList<>();

        if (nom != null && !nom.trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(salle.<String>get("nom")), "%" + nom.trim().toLowerCase() + "%"));
        }

        if (capaciteMin != null) {
            predicates.add(cb.ge(salle.<Integer>get("capacite"), capaciteMin));
        }

        if (debut != null && fin != null) {
            Subquery<Long> subquery = cq.subquery(Long.class);
            Root<Reservation> reservation = subquery.from(Reservation.class);
            subquery.select(reservation.<Salle>get("salle").<Long>get("id"));
            subquery.where(cb.and(
                    cb.lessThan(reservation.<LocalDateTime>get("debut"), fin),
                    cb.greaterThan(reservation.<LocalDateTime>get("fin"), debut)));

            predicates.add(cb.not(cb.in(salle.<Long>get("id")).value(subquery)));
        }

        return predicates;
    }
}
