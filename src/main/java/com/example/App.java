package com.example;

import com.example.model.Salle;
import com.example.service.SalleService;
import com.example.service.SalleServiceImpl;
import com.example.util.JpaUtil;
import com.example.util.PaginationResult;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Application de demonstration (Java SE).
 *
 * <p>L'enonce utilise {@code Persistence.createEntityManagerFactory("SallePU")}
 * directement ; on s'appuie ici sur {@link JpaUtil}, qui encapsule exactement
 * le meme appel derriere un singleton, pour eviter de recreer l'EMF dans
 * chaque classe.</p>
 *
 * <p>Le service est un EJB en production ({@code @Stateless}) ; hors conteneur
 * JEE, l'EntityManager lui est fourni par {@code setEntityManager(em)}, comme
 * le montre l'enonce.</p>
 */
public class App {

    public static void main(String[] args) {
        EntityManagerFactory emf = JpaUtil.getEntityManagerFactory();
        EntityManager em = emf.createEntityManager();

        try {
            SalleService salleService = new SalleServiceImpl();
            // Injection manuelle : hors conteneur, aucun EJB n'est disponible.
            ((SalleServiceImpl) salleService).setEntityManager(em);

            preparerDonnees(salleService, em);

            LocalDateTime debut = LocalDateTime.of(2024, 6, 1, 9, 0);
            LocalDateTime fin = LocalDateTime.of(2024, 6, 1, 11, 0);

            List<Salle> sallesDisponibles = salleService.getAvailableRooms(debut, fin);
            System.out.println("Salles disponibles entre " + debut + " et " + fin + ":");
            for (Salle salle : sallesDisponibles) {
                System.out.println("- " + salle.getNom() + " (Capacité: " + salle.getCapacite() + ")");
            }

            System.out.println();
            demontrerRechercheMultiCriteres((SalleServiceImpl) salleService);
        } finally {
            em.close();
            JpaUtil.close();
        }
    }

    /**
     * Jeu de donnees deterministe : deux salles sont occupees sur tout ou
     * partie du creneau 09h-11h, une troisieme est libre juste avant (08h-09h)
     * et une quatrieme juste apres (11h-13h) : les creneaux jointifs ne
     * comptent pas comme un chevauchement.
     */
    private static void preparerDonnees(SalleService service, EntityManager em) {
        em.getTransaction().begin();
        em.createQuery("DELETE FROM Reservation").executeUpdate();
        em.createQuery("DELETE FROM Salle").executeUpdate();
        em.getTransaction().commit();

        Long amphiA = service.creerSalle("Amphi A", 120);
        Long salleB12 = service.creerSalle("Salle B12", 30);
        Long salleC3 = service.creerSalle("Salle C3", 24);
        Long salleD1 = service.creerSalle("Salle D1", 45);
        service.creerSalle("Salle E7", 60);
        service.creerSalle("Amphi B", 200);

        // Amphi A : occupe sur tout le creneau recherche.
        service.reserver(amphiA, LocalDateTime.of(2024, 6, 1, 9, 0), LocalDateTime.of(2024, 6, 1, 11, 0));
        // Salle B12 : chevauchement partiel (10h-12h).
        service.reserver(salleB12, LocalDateTime.of(2024, 6, 1, 10, 0), LocalDateTime.of(2024, 6, 1, 12, 0));
        // Salle C3 : creneau jointif avant (08h-09h) -> reste disponible.
        service.reserver(salleC3, LocalDateTime.of(2024, 6, 1, 8, 0), LocalDateTime.of(2024, 6, 1, 9, 0));
        // Salle D1 : creneau jointif apres (11h-13h) -> reste disponible.
        service.reserver(salleD1, LocalDateTime.of(2024, 6, 1, 11, 0), LocalDateTime.of(2024, 6, 1, 13, 0));
    }

    /** Recherche multi-criteres (nom + capacite minimale + disponibilite) paginee. */
    private static void demontrerRechercheMultiCriteres(SalleServiceImpl service) {
        LocalDateTime debut = LocalDateTime.of(2024, 6, 1, 9, 0);
        LocalDateTime fin = LocalDateTime.of(2024, 6, 1, 11, 0);
        int taille = 2;

        System.out.println("Recherche des salles disponibles contenant \"salle\" "
                + "avec une capacite >= 20, par pages de " + taille + " :");

        int page = 1;
        PaginationResult<Salle> resultat;
        do {
            resultat = service.rechercherSallesPaginees("salle", 20, debut, fin, page, taille);
            System.out.println("Page " + resultat.getCurrentPage() + "/" + resultat.getTotalPages()
                    + " (" + resultat.getTotalItems() + " resultat(s) au total) :");
            for (Salle salle : resultat.getItems()) {
                System.out.println("- " + salle.getNom() + " (Capacité: " + salle.getCapacite() + ")");
            }
            page++;
        } while (page <= resultat.getTotalPages());
    }
}
