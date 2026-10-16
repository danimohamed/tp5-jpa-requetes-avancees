package com.example.repository;

import com.example.model.Salle;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Acces aux donnees des salles. Toutes les requetes avancees du TP sont
 * regroupees ici, ce qui isole la couche metier du detail de JPA.
 */
public interface SalleRepository {

    /**
     * Salles qui ne sont l'objet d'aucune reservation chevauchant le creneau.
     * Implemente en JPQL (sous-requete {@code NOT IN}).
     */
    List<Salle> findAvailableRooms(LocalDateTime debut, LocalDateTime fin);

    /**
     * Recherche multi-criteres dynamique, paginee.
     *
     * @param nom         filtre partiel et insensible a la casse, ou {@code null}
     * @param capaciteMin capacite minimale, ou {@code null}
     * @param debut       debut du creneau de disponibilite, ou {@code null}
     * @param fin         fin du creneau de disponibilite, ou {@code null}
     * @param page        numero de page, base 1
     * @param size        taille de page
     */
    List<Salle> searchRooms(String nom, Integer capaciteMin,
                            LocalDateTime debut, LocalDateTime fin,
                            int page, int size);

    /**
     * Nombre total de salles correspondant aux memes criteres que
     * {@link #searchRooms}, sans pagination.
     */
    long countRooms(String nom, Integer capaciteMin,
                    LocalDateTime debut, LocalDateTime fin);
}
