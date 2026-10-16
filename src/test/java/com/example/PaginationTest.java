package com.example;

import com.example.model.Salle;
import com.example.service.SalleServiceImpl;
import com.example.util.PaginationResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Etape 8 du TP : pagination (setFirstResult / setMaxResults), requete de
 * comptage et coherence entre les deux.
 */
public class PaginationTest {

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

    private List<String> nomsPage(String nom, Integer capaciteMin, int page, int size) {
        List<String> noms = new ArrayList<>();
        for (Salle salle : service.searchRooms(nom, capaciteMin,
                TestSupport.CRENEAU_DEBUT, TestSupport.CRENEAU_FIN, page, size)) {
            noms.add(salle.getNom());
        }
        return noms;
    }

    @Test
    public void premierePageContientLesPremiersResultatsTries() {
        assertEquals(Arrays.asList("Amphi B", "Salle C3"), nomsPage(null, null, 1, 2));
    }

    @Test
    public void deuxiemePageContientLesResultatsSuivants() {
        assertEquals(Arrays.asList("Salle D1", "Salle E7"), nomsPage(null, null, 2, 2));
    }

    @Test
    public void pageAuDelaDesResultatsEstVide() {
        assertTrue(nomsPage(null, null, 3, 2).isEmpty());
        assertTrue(nomsPage(null, null, 99, 2).isEmpty());
    }

    @Test
    public void tailleDePageSuperieureAuNombreDeResultatsRetourneTout() {
        assertEquals(4, nomsPage(null, null, 1, 100).size());
    }

    @Test
    public void paginationNeRetourneJamaisDeDoublon() {
        List<String> toutes = new ArrayList<>();
        toutes.addAll(nomsPage(null, null, 1, 2));
        toutes.addAll(nomsPage(null, null, 2, 2));

        assertEquals(4, toutes.size());
        assertEquals(4, new java.util.HashSet<>(toutes).size());
    }

    @Test
    public void lesCriteresSontAppliquesAvantLaPagination() {
        // "salle" + capacite >= 24 : Salle C3, Salle D1, Salle E7.
        assertEquals(Arrays.asList("Salle C3", "Salle D1"), nomsPage("salle", 24, 1, 2));
        assertEquals(Arrays.asList("Salle E7"), nomsPage("salle", 24, 2, 2));
    }

    @Test
    public void leComptageCorrespondAuNombreDeResultatsDeLaSelection() {
        long total = service.countRooms(null, null, TestSupport.CRENEAU_DEBUT, TestSupport.CRENEAU_FIN);

        assertEquals(4, total);
        assertEquals(total, nomsPage(null, null, 1, 100).size());
    }

    @Test
    public void leComptageRespecteLesCriteres() {
        assertEquals(3, service.countRooms("salle", null, TestSupport.CRENEAU_DEBUT, TestSupport.CRENEAU_FIN));
        assertEquals(2, service.countRooms("salle", 45, TestSupport.CRENEAU_DEBUT, TestSupport.CRENEAU_FIN));
        assertEquals(0, service.countRooms("inexistante", null, TestSupport.CRENEAU_DEBUT, TestSupport.CRENEAU_FIN));
    }

    @Test
    public void laSommeDesPagesEgaleLeComptageTotal() {
        long total = service.countRooms(null, null, TestSupport.CRENEAU_DEBUT, TestSupport.CRENEAU_FIN);
        int taille = 3;
        int recuperees = 0;
        int page = 1;

        while (true) {
            List<Salle> items = service.searchRooms(null, null,
                    TestSupport.CRENEAU_DEBUT, TestSupport.CRENEAU_FIN, page, taille);
            if (items.isEmpty()) {
                break;
            }
            recuperees += items.size();
            page++;
        }

        assertEquals(total, recuperees);
    }

    @Test
    public void paginationResultCalculeLeNombreDePagesAvecReste() {
        PaginationResult<String> resultat = new PaginationResult<>(Arrays.asList("a", "b", "c", "d", "e"), 1, 2, 5);

        assertEquals(3, resultat.getTotalPages());
        assertEquals(5, resultat.getTotalItems());
        assertEquals(1, resultat.getCurrentPage());
        assertEquals(2, resultat.getPageSize());
    }

    @Test
    public void paginationResultCalculeLeNombreDePagesSansReste() {
        assertEquals(3, new PaginationResult<>(Arrays.asList("a", "b", "c"), 1, 1, 3).getTotalPages());
        assertEquals(2, new PaginationResult<>(Arrays.asList("a", "b", "c", "d"), 1, 2, 4).getTotalPages());
    }

    @Test
    public void paginationResultSurBaseVide() {
        PaginationResult<String> resultat = new PaginationResult<>(new ArrayList<>(), 1, 10, 0);

        assertEquals(0, resultat.getTotalPages());
        assertEquals(0, resultat.getTotalItems());
        assertTrue(resultat.isEmpty());
        assertTrue(resultat.isFirstPage());
        assertTrue(resultat.isLastPage());
    }

    @Test
    public void paginationResultDistinguePremiereEtDernierePage() {
        PaginationResult<String> premiere = new PaginationResult<>(Arrays.asList("a", "b"), 1, 2, 6);
        PaginationResult<String> derniere = new PaginationResult<>(Arrays.asList("e", "f"), 3, 2, 6);

        assertTrue(premiere.isFirstPage());
        assertFalse(premiere.isLastPage());
        assertFalse(derniere.isFirstPage());
        assertTrue(derniere.isLastPage());
        assertFalse(premiere.isEmpty());
    }

    @Test
    public void paginationResultNeDivisePasParZeroAvecUneTailleInvalide() {
        PaginationResult<String> resultat = new PaginationResult<>(new ArrayList<>(), 1, 0, 0);

        assertEquals(0, resultat.getTotalPages());
        assertEquals(0, resultat.getTotalItems());
    }

    @Test
    public void rechercherSallesPagineesAssembleLesMetadonnees() {
        PaginationResult<Salle> page1 = service.rechercherSallesPaginees(
                "salle", 20, TestSupport.CRENEAU_DEBUT, TestSupport.CRENEAU_FIN, 1, 2);

        assertEquals(1, page1.getCurrentPage());
        assertEquals(2, page1.getPageSize());
        assertEquals(3, page1.getTotalItems());
        assertEquals(2, page1.getTotalPages());
        assertEquals(Arrays.asList("Salle C3", "Salle D1"), TestSupport.nomsTries(page1.getItems()));

        PaginationResult<Salle> page2 = service.rechercherSallesPaginees(
                "salle", 20, TestSupport.CRENEAU_DEBUT, TestSupport.CRENEAU_FIN, 2, 2);

        assertTrue(page2.isLastPage());
        assertEquals(Arrays.asList("Salle E7"), TestSupport.nomsTries(page2.getItems()));
    }
}
