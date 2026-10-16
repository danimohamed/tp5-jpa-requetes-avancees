package com.example.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Reservation d'une salle sur un creneau horaire.
 *
 * <p>La salle est obligatoire ({@code nullable = false} sur la colonne
 * {@code salle_id}) : une reservation orpheline n'a pas de sens metier.</p>
 */
@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salle_id", nullable = false)
    private Salle salle;

    @Column(nullable = false)
    private LocalDateTime debut;

    @Column(nullable = false)
    private LocalDateTime fin;

    public Reservation() {
    }

    public Reservation(LocalDateTime debut, LocalDateTime fin) {
        this.debut = debut;
        this.fin = fin;
    }

    public Reservation(Salle salle, LocalDateTime debut, LocalDateTime fin) {
        this.salle = salle;
        this.debut = debut;
        this.fin = fin;
    }

    /**
     * Deux creneaux se chevauchent des lors que le debut de l'un precede la fin
     * de l'autre et inversement. C'est exactement la condition utilisee par la
     * sous-requete JPQL et par la sous-requete Criteria du repository.
     */
    public boolean chevauche(LocalDateTime autreDebut, LocalDateTime autreFin) {
        return autreDebut.isBefore(fin) && autreFin.isAfter(debut);
    }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Salle getSalle() { return salle; }

    public void setSalle(Salle salle) { this.salle = salle; }

    public LocalDateTime getDebut() { return debut; }

    public void setDebut(LocalDateTime debut) { this.debut = debut; }

    public LocalDateTime getFin() { return fin; }

    public void setFin(LocalDateTime fin) { this.fin = fin; }

    @Override
    public String toString() {
        return "Reservation{id=" + id + ", debut=" + debut + ", fin=" + fin + '}';
    }
}
