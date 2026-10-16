package com.example;

import com.example.model.Salle;
import com.example.service.SalleServiceImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Etape 6 du TP : requete JPQL de disponibilite.
 *
 * <p>La regle de chevauchement est stricte ({@code :debut < r.fin AND :fin >
 * r.debut}) : deux creneaux jointifs ne sont donc pas en conflit.</p>
 */
public class SalleDisponibiliteTest {

    private EntityManager em;
    private SalleServiceImpl service;

    @Before
    public void setUp() {
        em = TestSupport.openEntityManager();
        service = TestSupport.newService(em);
        TestSupport.cleanDatabase(em);
    }

    @After
    public void tearDown() {
        TestSupport.cleanDatabase(em);
        em.close();
    }

    @Test
    public void baseVideAucuneSalleDisponible() {
        assertTrue(service.getAvailableRooms(TestSupport.CRENEAU_DEBUT, TestSupport.CRENEAU_FIN).isEmpty());
    }

    @Test
    public void toutesLesSallesSontDisponiblesSansReservation() {
        service.creerSalle("Salle A", 10);
        service.creerSalle("Salle B", 20);

        List<Salle> disponibles = service.getAvailableRooms(TestSupport.CRENEAU_DEBUT, TestSupport.CRENEAU_FIN);

        assertEquals(2, disponibles.size());
    }

    @Test
    public void reservationIdentiqueExclutLaSalle() {
        Long id = service.creerSalle("Amphi A", 120);
        service.reserver(id, TestSupport.CRENEAU_DEBUT, TestSupport.CRENEAU_FIN);

        assertTrue(service.getAvailableRooms(TestSupport.CRENEAU_DEBUT, TestSupport.CRENEAU_FIN).isEmpty());
    }

    @Test
    public void chevauchementPartielExclutLaSalle() {
        Long id = service.creerSalle("Salle B12", 30);
        // 10h-12h chevauche 09h-11h sur une heure.
        service.reserver(id, LocalDateTime.of(2024, 6, 1, 10, 0), LocalDateTime.of(2024, 6, 1, 12, 0));

        assertTrue(service.getAvailableRooms(TestSupport.CRENEAU_DEBUT, TestSupport.CRENEAU_FIN).isEmpty());
    }

    @Test
    public void creneauEnglobantExclutLaSalle() {
        Long id = service.creerSalle("Amphi B", 200);
        // 08h-14h englobe entierement 09h-11h.
        service.reserver(id, LocalDateTime.of(2024, 6, 1, 8, 0), LocalDateTime.of(2024, 6, 1, 14, 0));

        assertTrue(service.getAvailableRooms(TestSupport.CRENEAU_DEBUT, TestSupport.CRENEAU_FIN).isEmpty());
    }

    @Test
    public void creneauJointifAvantLaisseLaSalleDisponible() {
        Long id = service.creerSalle("Salle C3", 24);
        // 08h-09h se termine exactement quand la recherche commence.
        service.reserver(id, LocalDateTime.of(2024, 6, 1, 8, 0), LocalDateTime.of(2024, 6, 1, 9, 0));

        List<Salle> disponibles = service.getAvailableRooms(TestSupport.CRENEAU_DEBUT, TestSupport.CRENEAU_FIN);

        assertEquals(1, disponibles.size());
        assertEquals("Salle C3", disponibles.get(0).getNom());
    }

    @Test
    public void creneauJointifApresLaisseLaSalleDisponible() {
        Long id = service.creerSalle("Salle D1", 45);
        // 11h-13h commence exactement quand la recherche se termine.
        service.reserver(id, LocalDateTime.of(2024, 6, 1, 11, 0), LocalDateTime.of(2024, 6, 1, 13, 0));

        List<Salle> disponibles = service.getAvailableRooms(TestSupport.CRENEAU_DEBUT, TestSupport.CRENEAU_FIN);

        assertEquals(1, disponibles.size());
        assertEquals("Salle D1", disponibles.get(0).getNom());
    }

    @Test
    public void reservationSurUnAutreJourNImpactePasLaDisponibilite() {
        Long id = service.creerSalle("Salle E7", 60);
        service.reserver(id, LocalDateTime.of(2024, 6, 2, 9, 0), LocalDateTime.of(2024, 6, 2, 11, 0));

        assertEquals(1, service.getAvailableRooms(TestSupport.CRENEAU_DEBUT, TestSupport.CRENEAU_FIN).size());
    }

    @Test
    public void plusieursReservationsSurLaMemeSalleSontToutesPriseEnCompte() {
        Long id = service.creerSalle("Salle F2", 35);
        service.reserver(id, LocalDateTime.of(2024, 6, 1, 6, 0), LocalDateTime.of(2024, 6, 1, 7, 0));
        service.reserver(id, LocalDateTime.of(2024, 6, 1, 9, 30), LocalDateTime.of(2024, 6, 1, 10, 0));
        service.reserver(id, LocalDateTime.of(2024, 6, 1, 15, 0), LocalDateTime.of(2024, 6, 1, 16, 0));

        assertTrue(service.getAvailableRooms(TestSupport.CRENEAU_DEBUT, TestSupport.CRENEAU_FIN).isEmpty());
    }

    @Test
    public void jeuDeDonneesDeReferenceRetourneLesQuatreSallesAttendues() {
        TestSupport.seedStandardDataset(service);
        em.clear();

        List<Salle> disponibles = service.getAvailableRooms(TestSupport.CRENEAU_DEBUT, TestSupport.CRENEAU_FIN);

        assertEquals(
                Arrays.asList("Amphi B", "Salle C3", "Salle D1", "Salle E7"),
                TestSupport.nomsTries(disponibles));

        // Les deux salles occupees ne doivent jamais apparaitre.
        for (Salle salle : disponibles) {
            assertFalse("Amphi A est occupe", "Amphi A".equals(salle.getNom()));
            assertFalse("Salle B12 est occupee", "Salle B12".equals(salle.getNom()));
        }
    }

    @Test
    public void creneauPlusLargeReduitLeNombreDeSallesDisponibles() {
        TestSupport.seedStandardDataset(service);
        em.clear();

        // 08h-12h recouvre les quatre reservations (08h-09h, 09h-11h,
        // 10h-12h et 11h-13h) : seules les deux salles jamais reservees restent.
        List<Salle> disponibles = service.getAvailableRooms(
                LocalDateTime.of(2024, 6, 1, 8, 0), LocalDateTime.of(2024, 6, 1, 12, 0));

        assertEquals(Arrays.asList("Amphi B", "Salle E7"), TestSupport.nomsTries(disponibles));
    }

    @Test
    public void creneauHorsDesReservationsRetourneToutesLesSalles() {
        TestSupport.seedStandardDataset(service);
        em.clear();

        List<Salle> disponibles = service.getAvailableRooms(
                LocalDateTime.of(2024, 6, 1, 20, 0), LocalDateTime.of(2024, 6, 1, 21, 0));

        assertEquals(6, disponibles.size());
    }
}
