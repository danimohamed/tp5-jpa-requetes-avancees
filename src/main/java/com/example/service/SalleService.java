package com.example.service;

import com.example.model.Salle;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Couche metier : expose les cas d'usage attendus par l'application, sans
 * laisser fuir les details de JPA. Le service peut accueillir ici des regles
 * metier supplementaires avant de deleguer au repository.
 */
public interface SalleService {

    /** Salles libres sur tout le creneau demande. */
    List<Salle> getAvailableRooms(LocalDateTime debut, LocalDateTime fin);

    /** Recherche multi-criteres, paginee (page basee sur 1). */
    List<Salle> searchRooms(String nom, Integer capaciteMin,
                            LocalDateTime debut, LocalDateTime fin,
                            int page, int size);

    /** Nombre total de salles correspondant aux memes criteres. */
    long countRooms(String nom, Integer capaciteMin,
                    LocalDateTime debut, LocalDateTime fin);

    /** Persiste une salle et retourne son identifiant. */
    Long creerSalle(String nom, int capacite);

    /** Persiste une reservation pour une salle existante. */
    Long reserver(Long salleId, LocalDateTime debut, LocalDateTime fin);
}
