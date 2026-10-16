package com.example;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Verifie la sortie de la classe de demonstration {@link App} : c'est le
 * resultat attendu par l'enonce ("Salles disponibles entre ... :" puis une
 * ligne par salle).
 */
public class AppOutputTest {

    @Test
    public void laDemonstrationAfficheLesSallesDisponiblesSurLeCreneau() throws Exception {
        String sortie = executerApp();

        assertTrue("L'en-tete doit apparaitre sur sa propre ligne",
                Arrays.asList(sortie.split("\\R")).contains(
                        "Salles disponibles entre 2024-06-01T09:00 et 2024-06-01T11:00:"));

        assertTrue("Salle C3 doit etre disponible", sortie.contains("- Salle C3 (Capacité: 24)"));
        assertTrue("Salle D1 doit etre disponible", sortie.contains("- Salle D1 (Capacité: 45)"));
        assertTrue("Salle E7 doit etre disponible", sortie.contains("- Salle E7 (Capacité: 60)"));
        assertTrue("Amphi B doit etre disponible", sortie.contains("- Amphi B (Capacité: 200)"));
    }

    @Test
    public void laDemonstrationExclutLesSallesOccupees() throws Exception {
        String sortie = executerApp();

        assertFalse("Amphi A est occupe sur tout le creneau",
                sortie.contains("- Amphi A (Capacité: 120)"));
        assertFalse("Salle B12 chevauche le creneau",
                sortie.contains("- Salle B12 (Capacité: 30)"));
    }

    @Test
    public void laDemonstrationIllustreLaRechercheMultiCriteresPaginee() throws Exception {
        String sortie = executerApp();

        assertTrue(sortie.contains("Recherche des salles disponibles contenant \"salle\" "
                + "avec une capacite >= 20, par pages de 2 :"));
        assertTrue(sortie.contains("Page 1/2 (3 resultat(s) au total) :"));
        assertTrue(sortie.contains("Page 2/2 (3 resultat(s) au total) :"));
    }

    /** Execute {@link App#main} en capturant la sortie standard en UTF-8. */
    private String executerApp() throws Exception {
        PrintStream sortieOrigine = System.out;
        ByteArrayOutputStream tampon = new ByteArrayOutputStream();
        PrintStream capture = new PrintStream(tampon, true, StandardCharsets.UTF_8);
        System.setOut(capture);
        try {
            App.main(new String[0]);
        } finally {
            System.setOut(sortieOrigine);
            capture.flush();
        }
        return new String(tampon.toByteArray(), StandardCharsets.UTF_8);
    }
}
