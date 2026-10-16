# TP 5 — Requêtes avancées avec JPA : salles disponibles, recherche multi-critères et pagination

Projet Maven autonome illustrant les **requêtes avancées JPA/Hibernate** sur un domaine de
réservation de salles :

1. **JPQL avec sous-requête** — retrouver les salles disponibles sur un créneau donné, en
   excluant celles dont une réservation chevauche ce créneau (`NOT IN` + sous-requête).
2. **API Criteria** — recherche multi-critères combinant un nom partiel, une capacité minimale
   et la disponibilité, avec construction dynamique des prédicats.
3. **Pagination** — `setFirstResult` / `setMaxResults` accompagnés d'une requête de comptage
   qui applique **exactement les mêmes prédicats**, afin que le nombre de pages reste cohérent
   avec le contenu des pages.

Base de données **H2 en mémoire** — aucune installation externe n'est requise.

---

## Prérequis

| Outil | Version testée |
|-------|----------------|
| JDK   | 21 (le code cible Java 11) |
| Maven | 3.9.9 |

---

## Démarrage rapide

```bash
# Compiler et lancer les tests
mvn test

# Construire le jar
mvn package

# Exécuter la démonstration console
mvn clean compile exec:java -Dexec.mainClass="com.example.App"
```

Le jar produit est `target/salle-management-1.0.0.jar`.

---

## Structure du projet

```
tp5-jpa-requetes-avancees/
├── pom.xml
└── src
    ├── main
    │   ├── java/com/example
    │   │   ├── App.java                              # démonstration console
    │   │   ├── model
    │   │   │   ├── Salle.java                        # @OneToMany(mappedBy = "salle")
    │   │   │   └── Reservation.java                  # @ManyToOne + chevauche()
    │   │   ├── repository
    │   │   │   ├── SalleRepository.java              # findAvailableRooms / searchRooms / countRooms
    │   │   │   └── SalleRepositoryImpl.java          # JPQL + Criteria + pagination
    │   │   ├── service
    │   │   │   ├── SalleService.java                 # getAvailableRooms / searchRooms / countRooms
    │   │   │   └── SalleServiceImpl.java             # façade transactionnelle
    │   │   └── util
    │   │       ├── JpaUtil.java                      # EntityManagerFactory singleton
    │   │       └── PaginationResult.java             # page courante, total, nombre de pages
    │   └── resources/META-INF
    │       └── persistence.xml                       # unité de persistance "SallePU"
    └── test/java/com/example
        ├── TestSupport.java                          # remise à zéro + jeu de données de référence
        ├── SalleDisponibiliteTest.java               # 12 tests (JPQL de disponibilité)
        ├── RechercheMultiCriteresTest.java           # 15 tests (Criteria)
        ├── PaginationTest.java                       # 15 tests (pagination + comptage)
        └── AppOutputTest.java                        # 3 tests (sortie de la démonstration)
```

---

## Dépendances (`pom.xml`)

| Dépendance | Version | Portée | Rôle |
|------------|---------|--------|------|
| `javax.persistence-api` | 2.2 | compile | API JPA (annotations, JPQL, Criteria) |
| `hibernate-core` | 5.6.15.Final | compile | Implémentation JPA |
| `com.h2database:h2` | 2.1.214 | runtime | Base en mémoire |
| `javax.ejb:javax.ejb-api` | 3.2 | provided | Annotation `@Stateless` de l'énoncé |
| `javax.inject:javax.inject` | 1 | provided | Annotation `@Inject` de l'énoncé |
| `junit:junit` | 4.13.2 | test | Tests unitaires |
| `net.bytebuddy:byte-buddy` | 1.14.18 | compile | **Correctif JDK 21** (voir ci-dessous) |

> **Note sur les versions.** L'énoncé prescrit `hibernate-core 5.4.30.Final` et `h2 1.4.200`.
> Ces versions embarquent une version de Byte Buddy incapable de lire les fichiers de classe
> du JDK 21 (`Unsupported class file major version 65`). Hibernate est donc passé en
> `5.6.15.Final`, H2 en `2.1.214`, et `byte-buddy 1.14.18` est épinglé explicitement.
> Les packages `com.example.*`, le nom de l'unité de persistance `SallePU`, le nom des
> entités et des tables (`Salle`, `Reservation`) et l'URL JDBC
> `jdbc:h2:mem:test;DB_CLOSE_DELAY=-1` restent conformes à l'énoncé.

---

## Configuration JPA (`META-INF/persistence.xml`)

```xml
<persistence-unit name="SallePU" transaction-type="RESOURCE_LOCAL">
  <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>

  <class>com.example.model.Salle</class>
  <class>com.example.model.Reservation</class>

  <properties>
    <property name="javax.persistence.jdbc.driver" value="org.h2.Driver"/>
    <property name="javax.persistence.jdbc.url" value="jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"/>
    <property name="javax.persistence.jdbc.user" value="sa"/>
    <property name="hibernate.dialect" value="org.hibernate.dialect.H2Dialect"/>
    <property name="hibernate.hbm2ddl.auto" value="update"/>
    <property name="hibernate.show_sql" value="true"/>
    <property name="hibernate.format_sql" value="true"/>
  </properties>
</persistence-unit>
```

`DB_CLOSE_DELAY=-1` maintient la base en mémoire vivante pendant toute la JVM : le schéma et
les données **survivent à la fermeture de l'EntityManagerFactory**. Les tests doivent donc
nettoyer explicitement les tables entre chaque méthode (voir `TestSupport.cleanDatabase`).

---

## Points clés du TP

### 1. Disponibilité par sous-requête JPQL

```java
String jpql = "SELECT s FROM Salle s WHERE s.id NOT IN ("
        + "SELECT r.salle.id FROM Reservation r WHERE "
        + "(:debut < r.fin AND :fin > r.debut)"
        + ")";
```

La condition de chevauchement est **stricte** : un créneau qui se termine exactement quand la
recherche commence (ou qui commence exactement quand elle se termine) **n'est pas** un conflit.
C'est ce que vérifient `creneauJointifAvantLaisseLaSalleDisponible` et
`creneauJointifApresLaisseLaSalleDisponible`.

| Réservation | Créneau recherché 09h–11h | Résultat |
|-------------|---------------------------|----------|
| 09h–11h (identique) | chevauchement total | **exclue** |
| 10h–12h (partiel) | chevauchement d'une heure | **exclue** |
| 08h–14h (englobant) | chevauchement total | **exclue** |
| 08h–09h (jointif avant) | `09:00 < 09:00` est faux | disponible |
| 11h–13h (jointif après) | `11:00 > 11:00` est faux | disponible |
| autre jour | aucun recouvrement | disponible |

### 2. Recherche multi-critères avec l'API Criteria

Les prédicats sont construits dynamiquement, un par critère renseigné :

```java
if (nom != null && !nom.trim().isEmpty()) {
    predicates.add(cb.like(cb.lower(salle.get("nom")), "%" + nom.trim().toLowerCase() + "%"));
}
if (capaciteMin != null) {
    predicates.add(cb.ge(salle.get("capacite"), capaciteMin));
}
if (debut != null && fin != null) {
    Subquery<Long> subquery = cq.subquery(Long.class);
    Root<Reservation> reservation = subquery.from(Reservation.class);
    subquery.select(reservation.get("salle").get("id"));
    subquery.where(cb.and(
            cb.lessThan(reservation.get("debut"), fin),
            cb.greaterThan(reservation.get("fin"), debut)));

    predicates.add(cb.not(cb.in(salle.get("id")).value(subquery)));
}
```

Points d'attention :

* Un critère `null` (ou une chaîne vide pour le nom) **n'ajoute aucun prédicat** : passer
  `null, null` en `debut`/`fin` désactive entièrement le filtre de disponibilité.
* Le nom est comparé en **minuscules des deux côtés** (`cb.lower`) : la recherche est partielle
  et insensible à la casse.
* La capacité minimale est **inclusive** (`cb.ge`).
* Les prédicats sont combinés par `cb.and(...)` et les résultats triés par nom croissant, ce qui
  rend la pagination déterministe.

### 3. Pagination et comptage cohérents

```java
query.setFirstResult(Math.max(0, (page - 1) * size));
query.setMaxResults(size);
```

`searchRooms` et `countRooms` partagent la **même méthode privée `buildPredicates`** : le
comptage applique donc rigoureusement les mêmes filtres que la sélection. C'est la condition
pour que `totalPages = ceil(totalItems / pageSize)` soit exact — le test
`leComptageCorrespondAuNombreDeResultatsDeLaSelection` et
`laSommeDesPagesEgaleLeComptageTotal` le vérifient.

`PaginationResult<T>` encapsule le résultat :

| Méthode | Rôle |
|---------|------|
| `getItems()` | contenu de la page courante |
| `getCurrentPage()` / `getPageSize()` | position et taille demandées |
| `getTotalItems()` | nombre total de résultats **après filtrage** |
| `getTotalPages()` | `ceil(totalItems / pageSize)` |
| `isFirstPage()` / `isLastPage()` | aides d'affichage |

> **Décision d'implémentation.** Une taille de page `<= 0` donnerait une division par zéro :
> `getTotalPages()` retourne alors `0` au lieu de lever une exception, et
> `PaginationResultNeDivisePasParZeroAvecUneTailleInvalide` fige ce comportement. Le repository
> borne par ailleurs `setFirstResult` avec `Math.max(0, ...)` pour qu'une page `0` ou négative
> ne produise pas d'offset négatif.

### 4. Injection de l'EntityManager hors conteneur JEE

L'énoncé est écrit pour un conteneur JEE (`@Stateless`, `@PersistenceContext`, `@Inject`).
La démonstration et les tests sont des applications **Java SE** : l'EntityManager est donc
injecté manuellement, exactement comme le montre l'énoncé lui-même.

```java
SalleService salleService = new SalleServiceImpl();
((SalleServiceImpl) salleService).setEntityManager(em);   // injection manuelle
```

`javax.ejb-api` et `javax.inject` sont déclarés en portée **`provided`** uniquement pour que
les annotations de l'énoncé compilent ; ils ne sont pas embarqués dans le jar.

> **Décision d'implémentation — cycle de vie de l'EntityManager.** Dans l'énoncé, c'est `App`
> qui crée et ferme l'EntityManager et l'EntityManagerFactory. Le service ne doit donc jamais
> fermer l'EntityManager de son appelant. `creerSalle` et `reserver` ouvrent une transaction
> **uniquement s'ils en sont propriétaires** (`!em.getTransaction().isActive()`), la valident ou
> la rollback en cas d'erreur, et n'appellent jamais `em.close()`.

### 5. Modèle et intégrité référentielle

`Salle` porte un `@OneToMany(mappedBy = "salle", cascade = CascadeType.ALL)` et `Reservation`
un `@ManyToOne` avec `@JoinColumn(name = "salle_id", nullable = false)`. Les méthodes
`addReservation` / `removeReservation` maintiennent **les deux côtés** de la relation en
mémoire, ce qui évite les incohérences avant `flush()`. La table `reservations` portant la clé
étrangère, la remise à zéro supprime **d'abord `reservations`, puis `salles`**.

### 6. Bonnes pratiques retenues

1. **Partager la construction des prédicats** entre sélection et comptage : c'est la seule
   garantie que la pagination reste cohérente.
2. **Trier explicitement** (`ORDER BY`) dès qu'on pagine : sans ordre stable, deux pages
   peuvent renvoyer le même enregistrement.
3. **Borner l'offset** (`Math.max(0, ...)`) et documenter le comportement sur page invalide.
4. **Exprimer la disponibilité en base** (sous-requête) plutôt qu'en mémoire : le filtrage,
   le tri et la pagination restent faits par le SGBD.
5. **Utiliser `NOT IN` avec une sous-requête** pour exclure un ensemble, et `NOT EXISTS` comme
   alternative lorsque la sous-requête peut contenir `NULL`.
6. **Ne jamais fermer l'EntityManager d'un autre** : un service ne possède que la transaction
   qu'il a lui-même ouverte.

---

## Tests

```
Tests run: 45, Failures: 0, Errors: 0, Skipped: 0
```

Chaque test part d'une base vide (`TestSupport.cleanDatabase`) puis charge le **jeu de données
de référence** :

| Salle | Capacité | Réservation | Sur 09h–11h |
|-------|----------|-------------|-------------|
| Amphi A | 120 | 09h–11h | occupée |
| Salle B12 | 30 | 10h–12h | occupée (partiel) |
| Salle C3 | 24 | 08h–09h | disponible (jointif avant) |
| Salle D1 | 45 | 11h–13h | disponible (jointif après) |
| Salle E7 | 60 | — | disponible |
| Amphi B | 200 | — | disponible |

**`SalleDisponibiliteTest` (12 tests)** — requête JPQL `findAvailableRooms`

| Test | Vérifie |
|------|---------|
| `baseVideAucuneSalleDisponible` | aucune salle sans donnée |
| `toutesLesSallesSontDisponiblesSansReservation` | aucune réservation → tout est libre |
| `reservationIdentiqueExclutLaSalle` | chevauchement total |
| `chevauchementPartielExclutLaSalle` | 10h–12h exclut la salle |
| `creneauEnglobantExclutLaSalle` | 08h–14h exclut la salle |
| `creneauJointifAvantLaisseLaSalleDisponible` | 08h–09h ne bloque pas 09h–11h |
| `creneauJointifApresLaisseLaSalleDisponible` | 11h–13h ne bloque pas 09h–11h |
| `reservationSurUnAutreJourNImpactePasLaDisponibilite` | isolation par date |
| `plusieursReservationsSurLaMemeSalleSontToutesPriseEnCompte` | 3 réservations non chevauchantes |
| `jeuDeDonneesDeReferenceRetourneLesQuatreSallesAttendues` | Amphi B, C3, D1, E7 |
| `creneauPlusLargeReduitLeNombreDeSallesDisponibles` | 08h–12h → Amphi B, Salle E7 |
| `creneauHorsDesReservationsRetourneToutesLesSalles` | 20h–21h → 6 salles |

**`RechercheMultiCriteresTest` (15 tests)** — API Criteria `searchRooms`

| Test | Vérifie |
|------|---------|
| `sansCritereNomNiCapaciteSeuleLaDisponibiliteSApplique` | seul le filtre de créneau agit |
| `sansAucunCritereToutesLesSallesSontRetourneesTrieesParNom` | `null, null` → 6 salles triées |
| `critereNomEstPartielEtInsensibleALaCasse` | `"salle"` → 4 salles, `"B12"` → 1 |
| `critereNomEnMajusculesDonneLeMemeResultat` | `"SALLE"` == `"salle"` |
| `critereNomVideEstIgnore` | chaîne vide et espaces → ignorés |
| `critereCapaciteMinimaleFiltreLesPetitesSalles` | capacité ≥ 50 → 3 salles |
| `capaciteMinimaleEstInclusive` | capacité ≥ 30 inclut Salle B12 |
| `capaciteMinimaleTropEleveeNeRetourneRien` | capacité ≥ 500 → vide |
| `critereDisponibiliteExclutLesSallesOccupees` | Amphi A et Salle B12 absentes |
| `combinaisonNomCapaciteEtDisponibilite` | 3 critères cumulés |
| `combinaisonNomEtCapaciteSansContrainteDeDisponibilite` | créneau `null` → salles occupées incluses |
| `critereDisponibiliteElimineLaSeuleSalleCorrespondante` | nom + créneau → vide |
| `creneauSansConflitRetourneToutesLesSallesCorrespondantes` | autre jour → tout |
| `resultatsTriesParNomCroissant` | ordre stable exigé par la pagination |
| `leRepositoryExposeLesMemesResultatsQueLeService` | service et repository cohérents |

**`PaginationTest` (15 tests)** — pagination et comptage

| Test | Vérifie |
|------|---------|
| `premierePageContientLesPremiersResultatsTries` | page 1, taille 2 |
| `deuxiemePageContientLesResultatsSuivants` | page 2, taille 2 |
| `pageAuDelaDesResultatsEstVide` | page 99 → vide |
| `tailleDePageSuperieureAuNombreDeResultatsRetourneTout` | taille 100 → tout |
| `paginationNeRetourneJamaisDeDoublon` | union des pages sans répétition |
| `lesCriteresSontAppliquesAvantLaPagination` | filtrage puis découpage |
| `leComptageCorrespondAuNombreDeResultatsDeLaSelection` | `countRooms` == taille réelle |
| `leComptageRespecteLesCriteres` | `"salle"` → 3, capacité ≥ 45 → 2 |
| `laSommeDesPagesEgaleLeComptageTotal` | Σ pages == `totalItems` |
| `paginationResultCalculeLeNombreDePagesAvecReste` | 7 / 3 → 3 pages |
| `paginationResultCalculeLeNombreDePagesSansReste` | 6 / 3 → 2 pages |
| `paginationResultSurBaseVide` | 0 élément → 0 page |
| `paginationResultDistinguePremiereEtDernierePage` | `isFirstPage` / `isLastPage` |
| `paginationResultNeDivisePasParZeroAvecUneTailleInvalide` | taille 0 → 0 page |
| `rechercherSallesPagineesAssembleLesMetadonnees` | assemblage complet du résultat |

**`AppOutputTest` (3 tests)** — sortie console de la démonstration

| Test | Vérifie |
|------|---------|
| `laDemonstrationAfficheLesSallesDisponiblesSurLeCreneau` | en-tête + les 4 salles libres |
| `laDemonstrationExclutLesSallesOccupees` | Amphi A et Salle B12 absentes |
| `laDemonstrationIllustreLaRechercheMultiCriteresPaginee` | « Page 1/2 » et « Page 2/2 » |

---

## Résultat de la démonstration

```
Salles disponibles entre 2024-06-01T09:00 et 2024-06-01T11:00:
- Salle C3 (Capacité: 24)
- Salle D1 (Capacité: 45)
- Salle E7 (Capacité: 60)
- Amphi B (Capacité: 200)

Recherche des salles disponibles contenant "salle" avec une capacite >= 20, par pages de 2 :
Page 1/2 (3 resultat(s) au total) :
- Salle C3 (Capacité: 24)
- Salle D1 (Capacité: 45)
Page 2/2 (3 resultat(s) au total) :
- Salle E7 (Capacité: 60)
```

Amphi A (09h–11h) et Salle B12 (10h–12h) sont bien écartées du créneau 09h–11h, tandis que
Salle C3 (08h–09h) et Salle D1 (11h–13h) restent disponibles : leurs réservations sont
**jointives**, pas chevauchantes.

---

## Auteur

Mohamed Dani — [@danimohamed](https://github.com/danimohamed)
