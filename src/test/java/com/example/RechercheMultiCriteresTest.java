package com.example;

import com.example.model.Salle;
import com.example.repository.SalleRepository;
import com.example.service.SalleServiceImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Etape 7 du TP : recherche multi-criteres avec l'API Criteria
 * (nom, capacite minimale, disponibilite).
 */
public class RechercheMultiCriteresTest {

    private EntityManager em;
    private SalleServiceImpl service;

    @Before
    public void setUp() {
        em = TestSupport.openEntityManager();
        service = TestSupport.newService(em);
        TestSupport.cleanDatabase(em);
        TestSupport.seedStandardDataset(service);
        em.clear();
    }

    @After
    public void tearDown() {
        TestSupport.cleanDatabase(em);
        em.close();
    }

    private List<Salle> rechercher(String nom, Integer capaciteMin) {
        return service.searchRooms(nom, capaciteMin,
                TestSupport.CRENEAU_DEBUT, TestSupport.CRENEAU_FIN, 1, 50);
    }

    @Test
    public void sansCritereNomNiCapaciteSeuleLaDisponibiliteSApplique() {
        List<Salle> resultats = rechercher(null, null);

        assertEquals(Arrays.asList("Amphi B", "Salle C3", "Salle D1", "Salle E7"),
                TestSupport.nomsTries(resultats));
    }

    @Test
    public void sansAucunCritereToutesLesSallesSontRetourneesTrieesParNom() {
        List<Salle> resultats = service.searchRooms(null, null, null, null, 1, 50);

        assertEquals(Arrays.asList("Amphi A", "Amphi B", "Salle B12", "Salle C3", "Salle D1", "Salle E7"),
                TestSupport.nomsTries(resultats));
    }

    @Test
    public void critereNomEstPartielEtInsensibleALaCasse() {
        List<Salle> resultats = rechercher("salle", null);

        assertEquals(Arrays.asList("Salle C3", "Salle D1", "Salle E7"), TestSupport.nomsTries(resultats));
    }

    @Test
    public void critereNomEnMajusculesDonneLeMemeResultat() {
        assertEquals(TestSupport.nomsTries(rechercher("salle", null)),
                TestSupport.nomsTries(rechercher("SALLE", null)));
    }

    @Test
    public void critereNomVideEstIgnore() {
        assertEquals(rechercher(null, null).size(), rechercher("   ", null).size());
    }

    @Test
    public void critereCapaciteMinimaleFiltreLesPetitesSalles() {
        List<Salle> resultats = rechercher(null, 100);

        // Amphi A (120) est occupe sur le creneau, Amphi B (200) ne l'est pas.
        assertEquals(Arrays.asList("Amphi B"), TestSupport.nomsTries(resultats));
    }

    @Test
    public void capaciteMinimaleEstInclusive() {
        List<Salle> resultats = rechercher(null, 45);

        assertTrue("Salle D1 (45 places) doit etre incluse",
                TestSupport.nomsTries(resultats).contains("Salle D1"));
        assertTrue("Salle C3 (24 places) doit etre exclue",
                !TestSupport.nomsTries(resultats).contains("Salle C3"));
    }

    @Test
    public void capaciteMinimaleTropEleveeNeRetourneRien() {
        assertTrue(rechercher(null, 500).isEmpty());
    }

    @Test
    public void critereDisponibiliteExclutLesSallesOccupees() {
        // Salle B12 (10h-12h) chevauche le creneau ; Salle C3 et Salle D1 sont
        // jointives, donc disponibles.
        List<Salle> resultats = rechercher("salle", null);

        assertEquals(Arrays.asList("Salle C3", "Salle D1", "Salle E7"),
                TestSupport.nomsTries(resultats));
    }

    @Test
    public void combinaisonNomCapaciteEtDisponibilite() {
        List<Salle> resultats = rechercher("salle", 30);

        // Salle B12 (30) est ecartee par le critere de disponibilite.
        assertEquals(Arrays.asList("Salle D1", "Salle E7"), TestSupport.nomsTries(resultats));
    }

    @Test
    public void combinaisonNomEtCapaciteSansContrainteDeDisponibilite() {
        List<Salle> resultats = service.searchRooms("amphi", 120, null, null, 1, 50);

        // Sans creneau, Amphi A (occupe) reste dans les resultats.
        assertEquals(Arrays.asList("Amphi A", "Amphi B"), TestSupport.nomsTries(resultats));
    }

    @Test
    public void critereDisponibiliteElimineLaSeuleSalleCorrespondante() {
        // Amphi A (120) est occupe : seul Amphi B reste.
        assertEquals(Arrays.asList("Amphi B"), TestSupport.nomsTries(rechercher("amphi", 120)));
    }

    @Test
    public void creneauSansConflitRetourneToutesLesSallesCorrespondantes() {
        List<Salle> resultats = service.searchRooms("salle", null,
                LocalDateTime.of(2024, 6, 1, 20, 0), LocalDateTime.of(2024, 6, 1, 21, 0), 1, 50);

        assertEquals(Arrays.asList("Salle B12", "Salle C3", "Salle D1", "Salle E7"),
                TestSupport.nomsTries(resultats));
    }

    @Test
    public void resultatsTriesParNomCroissant() {
        List<Salle> resultats = service.searchRooms(null, null,
                TestSupport.CRENEAU_DEBUT, TestSupport.CRENEAU_FIN, 1, 50);

        List<String> noms = new java.util.ArrayList<>();
        for (Salle salle : resultats) {
            noms.add(salle.getNom());
        }
        List<String> tries = new java.util.ArrayList<>(noms);
        java.util.Collections.sort(tries);
        assertEquals(tries, noms);
    }

    @Test
    public void leRepositoryExposeLesMemesResultatsQueLeService() {
        SalleRepository repository = TestSupport.newRepository(em);
        long viaRepository = repository.countRooms("salle", 20, TestSupport.CRENEAU_DEBUT, TestSupport.CRENEAU_FIN);
        long viaService = service.countRooms("salle", 20, TestSupport.CRENEAU_DEBUT, TestSupport.CRENEAU_FIN);

        assertEquals(viaService, viaRepository);
    }
}
