import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.testng.annotations.Parameters;

public class EsupEmargementIT {

    private EsupEmargementDriver esupEmargementDriver;

    private String expectedTitle = "Esup-emargement -- Application démargement";
    private int adeProjectId = -1;

    private String contextKeyPrefix   = null;
    private String contextTitlePrefix = null;

    private String defaultSiteName = null;

    /**
     * Commencer par supprimer automatiquement les contextes de test créés précédemment
     * en les identifiant par le début du titre tel que configuré dans le fichier testng-parameters.xml
     * REM: Ne fonctionne pas avec la v1.1.3
     */
    private boolean isSupprimerPrecedentsContextesTest = true;

    @Parameters({"test-server-url", "test-contexts-prefix", "site1-name", "ade-project-id", "admin1-login", "admin1-password"})
    @BeforeSuite
    public void setUp(
        String esupEmargementTestUrl,
        String testContextPrefix,
        String defaultSiteName,
        int adeProjectId,
        String admin1Login,
        String admin1Password
    ) {
        this.adeProjectId    = adeProjectId;
        this.defaultSiteName = defaultSiteName;

        SeleniumConfig config = new SeleniumConfig();

        HashMap<String, HashMap<String, String>> connectionParametersByProfile = new HashMap<String, HashMap<String, String>>();
        HashMap<String, String> connectionParameter = new HashMap<String, String>();
        connectionParameter.put("login", admin1Login);
        connectionParameter.put("password", admin1Password);
        connectionParametersByProfile.put("admin1", connectionParameter);

        CasDriver casDriver = new CasDriver(config.getDriver(), connectionParametersByProfile);
        esupEmargementDriver = new EsupEmargementDriver(casDriver, esupEmargementTestUrl);

        LocalDateTime currentDateTime = LocalDateTime.now();
        String currentDateTimeStr = currentDateTime.toString();
        contextTitlePrefix = testContextPrefix+" "+currentDateTime;
        contextKeyPrefix = currentDateTimeStr.substring(0, 4)+currentDateTimeStr.substring(5, 7)+currentDateTimeStr.substring(8, 10)+
                     currentDateTimeStr.substring(11, 13)+currentDateTimeStr.substring(14, 16)+currentDateTimeStr.substring(17, 19);

    }

    @AfterSuite
    public void tearDown() {
        esupEmargementDriver.quitterEsupEmargement();
    }

    //---------------------------------------------------------------------
    // CtxDefault : Paramétrage esup-emargement par défaut
    //---------------------------------------------------------------------
    @Test
    @Parameters({"cas-home-title"})
    public void ctxDefaultConnexion(String expectedCASTitle) {
        String actualTitle = esupEmargementDriver.getTitle();

        assertNotNull(actualTitle);
        assertEquals(actualTitle, expectedTitle);

        esupEmargementDriver.accederPageDeConnexionDepuisPageAccueil();
        actualTitle = esupEmargementDriver.getTitle();
        assertNotNull(actualTitle);
        assertEquals(actualTitle, expectedCASTitle);

        esupEmargementDriver.seConnecterDepuisPageCasParProfil("admin1");
    }

    @Test (dependsOnMethods={"ctxDefaultConnexion"})
    @Parameters({"admin1-eppn", "test-contexts-prefix"})
    public void cleanPreviousTestContexts(String adminEppn, String contextPrefix) {
        if (isSupprimerPrecedentsContextesTest) {
            try {
                esupEmargementDriver.supprimerContextesAvecTitreCommencantPar(contextPrefix);
            } catch (Exception e) {
                assertNull(e);
            }
        }
    }

    @Test (dependsOnMethods={"cleanPreviousTestContexts"})
    @Parameters({"admin1-eppn"})
    public void ctxDefaultNewContext(String adminEppn) {
        String contextKey = contextKeyPrefix+"_default";
        // TODO Reprendre les étapes de creerNouveauContexte() et ajouter les assertions qui vont bien
        esupEmargementDriver.creerNouveauContexte(contextTitlePrefix, contextKey);
        esupEmargementDriver.ajouterAdministrateurContexte(contextKey, adminEppn);
    }

    @Test (dependsOnMethods={"ctxDefaultNewContext"})
    public void ctxDefaultSetupContext() {
        String contextKey = contextKeyPrefix+"_default";
        // Pour être "sûr" de voir le nouveau contexte, se déconnecter/reconnecter
        esupEmargementDriver.seDeconnecter();
        esupEmargementDriver.seConnecterDepuisPageAccueilParProfil("admin1");
        esupEmargementDriver.selectionnerContexte(contextKey);

        esupEmargementDriver.ajouterSite(defaultSiteName, true);

        // TODO Ajouter au moins un pur gestionnaire
    }

    @Test (dependsOnMethods={"ctxDefaultSetupContext"})
    public void ctxDefaultCreerSession() {
        String nomSession = "Session de test";
        String dateSession = "2026-06-01";
        String heureDebut = "09:00";
        String heureFin   = "10:00";
        esupEmargementDriver.creerSession(nomSession, dateSession, heureDebut, heureFin);
        try {
            List<HashMap<String, String>> list = esupEmargementDriver.getSessions();
            assertEquals(list.size(), 1, "Le nb de résultats retournés par la recherche est incorrect");
            List<HashMap<String, String>> expectedList = new ArrayList<HashMap<String, String>>();
            HashMap<String, String> session = getExpectedSessionValues(
                nomSession,
                dateSession,
                heureDebut,
                heureFin,
                "Examen",
                "SALLE",
                0,
                0,
                0,
                null
            );
            expectedList.add(session);
            assertEquals(list, expectedList);
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test (dependsOnMethods={"ctxDefaultCreerSession"})
    @Parameters({"etudiant1-eppn"})
    public void ctxDefaultAjouterParticipantASession(String participantEppn) {
        String nomSession = "Session de test";
        String dateSession = "2026-06-01";
        String heureDebut = "09:00";
        String heureFin   = "10:00";
        esupEmargementDriver.ajouterParticipantASession(
            participantEppn,
            dateSession,
            heureDebut,
            nomSession,
            null);

        try {
            List<HashMap<String, String>> list = esupEmargementDriver.getSessions();
            assertEquals(list.size(), 1, "Le nb de résultats retournés par la recherche ADE est incorrect");
            List<HashMap<String, String>> expectedList = new ArrayList<HashMap<String, String>>();

            HashMap<String, String> session = getExpectedSessionValues(
                nomSession,
                dateSession,
                heureDebut,
                heureFin,
                "Examen",
                "SALLE",
                0,
                0,
                1,
                null
            );
            expectedList.add(session);
            assertEquals(list, expectedList);
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test (dependsOnMethods={"ctxDefaultAjouterParticipantASession"})
    @Parameters({"admin1-eppn", "etudiant1-eppn"})
    public void ctxDefaultCreerGroupe(String adminEppn, String participantEppn) {
        int anneeUniv = 2025;
        String nomGroupe = "L1 A1 - Gp 1";
        esupEmargementDriver.creerGroupe(nomGroupe, anneeUniv);

        try {
            List<HashMap<String, String>> groupes = esupEmargementDriver.getGroupes();
            assertEquals(groupes.size(), 1, "Le nb de groupes incorrect");

            List<HashMap<String, String>> expectedGroupes = new ArrayList<HashMap<String, String>>();
            HashMap<String, String> groupe = new HashMap<String, String>();
            groupe.put(EsupEmargementDriver.GROUPE_DESCRIPTION, nomGroupe);
            groupe.put(EsupEmargementDriver.GROUPE_MEMBRES, "0");
            groupe.put(EsupEmargementDriver.GROUPE_ANNEE, ""+anneeUniv);
            groupe.put(EsupEmargementDriver.GROUPE_MODIFICATEUR, adminEppn);
            groupe.put(EsupEmargementDriver.GROUPE_NOM, nomGroupe);
            expectedGroupes.add(groupe);

            assertEquals(groupes, expectedGroupes);
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test (dependsOnMethods={"ctxDefaultCreerGroupe"})
    @Parameters({"admin1-eppn", "etudiant1-eppn"})
    public void ctxDefaultImporterParticipantEsupEmargementDansGroupe(String adminEppn, String participantEppn) {
        int anneeUniv = 2025;
        String nomGroupe = "L1 A1 - Gp 1";

        // Les EPPNs doivent déjà être connus en tant que participant dans EsupEmargement
        // pour pouvoir être ajoutés ainsi dans le groupe (v1.1.3)
        ArrayList<String> eppns = new ArrayList<String>();
        eppns.add(participantEppn);

        esupEmargementDriver.importerParticipantsDansGroupe(nomGroupe, eppns);

        try {
            List<HashMap<String, String>> groupes = esupEmargementDriver.getGroupes();
            assertEquals(groupes.size(), 1, "Le nb de groupes incorrect");

            List<HashMap<String, String>> expectedGroupes = new ArrayList<HashMap<String, String>>();
            HashMap<String, String> groupe = new HashMap<String, String>();
            groupe.put(EsupEmargementDriver.GROUPE_DESCRIPTION, nomGroupe);
            groupe.put(EsupEmargementDriver.GROUPE_MEMBRES, ""+eppns.size());
            groupe.put(EsupEmargementDriver.GROUPE_ANNEE, ""+anneeUniv);
            groupe.put(EsupEmargementDriver.GROUPE_MODIFICATEUR, adminEppn);
            groupe.put(EsupEmargementDriver.GROUPE_NOM, nomGroupe);
            expectedGroupes.add(groupe);

            assertEquals(groupes, expectedGroupes);
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test (dependsOnMethods={"ctxDefaultImporterParticipantEsupEmargementDansGroupe"})
    public void ctxDefaultSupprimerSession() {
        String nomSession = "Session de test";
        String dateSession = "2026-06-01";
        String heureDebut = "09:00";
        esupEmargementDriver.supprimerSession(dateSession, heureDebut, nomSession);
    }

    //----------------------------------------------------------
    // Context 2: Configuration par défaut
    //            + paramètre DISPLAY_SESSION_GROUPS = true
    //
    // UseCase Ctx4: 
    //    - 1 groupe dans esup-emargement = 1 formation
    //----------------------------------------------------------
    @Test (dependsOnMethods={"ctxDefaultSupprimerSession"})
    @Parameters({"admin1-eppn", "gestionnaire1-ade-composante-name", "etudiant1-eppn", "etudiant2-eppn", "etudiant3-eppn"})
    public void ctx2AffichageColGroupesDansListeSession(
        String adminEppn,
        String nomComposanteGestionnaire,
        String participantEppn,
        String participantEppn2,
        String participantEppn3
    ) {
        String contextKey = this.contextKeyPrefix+"_affichageGrpDansListeSession";
        String contextTitle = this.contextTitlePrefix+" Affichage d'une colonne groupe dans la liste des sessions";

        creerEtInitialiserNouveauContexte(contextKey, contextTitle, adminEppn, defaultSiteName);
        esupEmargementDriver.configurer("AFFICHAGE", "DISPLAY_SESSION_GROUPS", true);

        // Création des groupes esup-emargement
        ArrayList<String> eppns = new ArrayList<String>();
        eppns.add(participantEppn);
        eppns.add(participantEppn2);
        creerGroupeEtImporterEppns("L1 Physique", eppns);

        eppns = new ArrayList<String>();
        eppns.add(participantEppn);
        eppns.add(participantEppn3);
        creerGroupeEtImporterEppns("L1 Chimie", eppns);

        eppns = new ArrayList<String>();
        eppns.add(participantEppn2);
        eppns.add(participantEppn3);
        creerGroupeEtImporterEppns("L1 Géo", eppns);

        // Test proprement dit
        //--------------------------------------
        String nomSession = "Cours L1 Chimie";
        String dateSession = "2026-06-26";
        String heureDebut = "09:00";
        String heureFin   = "10:00";
        esupEmargementDriver.creerSession(nomSession, dateSession, heureDebut, heureFin);

        esupEmargementDriver.ajouterParticipantASession(participantEppn, dateSession, heureDebut);
        esupEmargementDriver.ajouterParticipantASession(participantEppn3, dateSession, heureDebut);

        try {
            List<HashMap<String, String>> list = esupEmargementDriver.getSessions();
            assertEquals(list.size(), 1, "Le nb de résultats retournés par la recherche ADE est incorrect");
            List<HashMap<String, String>> expectedList = new ArrayList<HashMap<String, String>>();
            HashMap<String, String> session = getExpectedSessionValues(
                nomSession,
                dateSession,
                "09:00",
                "10:00",
                "Examen",
                "SALLE",
                0,
                0,
                2,
                "L1 Chimie"
            );
            
            expectedList.add(session);
            assertEquals(list, expectedList);
        } catch (Exception e) {
            assertNull(e);
        } 
    }

    //---------------------------------------------------------------------
    // Context Ade1 : Paramétrage esup-emargement par défaut + sélection id projet ADE
    //---------------------------------------------------------------------
    @Test (dependsOnMethods={"ctxDefaultSupprimerSession"})
    @Parameters({"admin1-eppn", "gestionnaire1-ade-composante-name"})
    public void ctxAde1SetupPrefsGestionnaire(String adminEppn, String nomComposanteGestionnaire) {
        String contextKey = contextKeyPrefix+"_ade";
        String contextTitle = contextTitlePrefix+" ADE";

        creerEtInitialiserNouveauContexte(contextKey, contextTitle, adminEppn, defaultSiteName);
        esupEmargementDriver.configurerAdeProjetParDefaut(adeProjectId);

        try {
            List<HashMap<String,String>> composantes = esupEmargementDriver.getAdePrefsComposantes();

            List<HashMap<String, String>> expectedComposantesList = new ArrayList<HashMap<String, String>>();
            HashMap<String, String> composante = new HashMap<String, String>();
            composante.put("Libellé", nomComposanteGestionnaire);
            expectedComposantesList.add(composante);

            assertEquals(composantes, expectedComposantesList, "La liste des composantes n'est pas celle attendue");

            //-----------------------
            // Quel est le rôle de la table ADE Campus Prefs: Salles ?
            // Est-ce que les salles sont censées être au premier niveau et non pas rangées dans des batiments ?
            //-----------------------
            List<HashMap<String,String>> salles = esupEmargementDriver.getAdePrefsSalles();

            List<HashMap<String, String>> expectedSallesList = new ArrayList<HashMap<String, String>>();
            HashMap<String, String> salle = new HashMap<String, String>();
            salle.put("Libellé", "Batiment A");
            expectedSallesList.add(salle);

            salle = new HashMap<String, String>();
            salle.put("Libellé", "Batiment B");
            expectedSallesList.add(salle);

            assertEquals(salles, expectedSallesList,  "La liste des salles n'est pas celle attendue");

        } catch (Exception e) {
            assertNull(e);
        }

        // TODO se connecter avec un pur profil gestionnaire
        esupEmargementDriver.configurerAdePrefsGestionnaire(nomComposanteGestionnaire);
    }

    @Test (dependsOnMethods={"ctxAde1SetupPrefsGestionnaire"})
    @Parameters({"admin1-eppn", "gestionnaire1-ade-composante-name"})
    public void ctxAde1LancerRechercheSessionsAde(String adminEppn, String nomComposanteGestionnaire) {
        String dateSession = "2026-06-29";
        String nomSession  = "Cours A1";
        esupEmargementDriver.lancerRechercheSessionsAde(
            nomComposanteGestionnaire,
            dateSession,
            dateSession, 
            nomSession
        );
        try {
            List<HashMap<String, String>> list = esupEmargementDriver.getListeSessionsAdeDepuisEcranResultatRecherche();
            assertEquals(list.size(), 1, "Le nb de résultats retournés par la recherche ADE est incorrect");
            List<HashMap<String, String>> expectedList = new ArrayList<HashMap<String, String>>();
            HashMap<String, String> sessionAde = new HashMap<String, String>();
            sessionAde.put(EsupEmargementDriver.SESSION_TYPE, "");
            sessionAde.put(EsupEmargementDriver.SESSION_NOM, nomSession);
            sessionAde.put(EsupEmargementDriver.SESSION_GROUPE, "[L1 A1 - Gp 1]");
            sessionAde.put(EsupEmargementDriver.SESSION_DATE, EsupEmargementDriver.getDateDDMMYYFromYYYYMMDD(dateSession));
            sessionAde.put(EsupEmargementDriver.SESSION_DEBUT, "09:00");
            sessionAde.put(EsupEmargementDriver.SESSION_FIN, "09:30");
            sessionAde.put(EsupEmargementDriver.SESSION_ENSEIGNANTS, "[Permanent A1]");
            sessionAde.put(EsupEmargementDriver.SESSION_SALLES, "[Salle A1]");
            sessionAde.put(EsupEmargementDriver.SESSION_FORMATION, "");
            expectedList.add(sessionAde);
            assertEquals(list, expectedList);
        } catch (Exception e) {
            assertNull(e);
        }
    }

    @Test (dependsOnMethods={"ctxAde1LancerRechercheSessionsAde"})
    public void ctxAde1ImporterSessionAde() {

        esupEmargementDriver.importerSessionAde();

        try {
            List<HashMap<String, String>> list = esupEmargementDriver.getSessions();
            assertEquals(list.size(), 1, "Le nb de résultats retournés par la recherche ADE est incorrect");
            List<HashMap<String, String>> expectedList = new ArrayList<HashMap<String, String>>();
            expectedList.add(getExpectedSessionValues(
                "Cours A1",
                "2026-06-29",
                "09:00",
                "09:30",
                "",
                "SESSION",
                1,
                1,
                0,
                null
            ));
            assertEquals(list, expectedList);
        } catch (Exception e) {
            assertNull(e);
        }

        HashMap<String, String> sessionDetails = esupEmargementDriver.getSessionFullDetails(0);
        HashMap<String, String> expectedSessionDetails = new HashMap<String, String>();
        expectedSessionDetails.put("Afficher groupes", "false");
        assertEquals(sessionDetails, expectedSessionDetails);
    }

    //---------------------------------------------------------------------------------
    // Context Ade 2: Configuration par défaut + sélection projet ADE
    //              + paramètre ADE_IMPORT_AFFICHER_GROUPES (affichage groupe par défaut sur import ADE) = true
    //---------------------------------------------------------------------------------
    @Test (dependsOnMethods={"ctxAde1ImporterSessionAde"})
    @Parameters({"admin1-eppn", "gestionnaire1-ade-composante-name"})
    public void ctxAde2importSessionTestCocherAutomatiquementAfficherGroupes(String adminEppn, String nomComposanteGestionnaire)
    {
        esupEmargementDriver.seDeconnecter();
        esupEmargementDriver.seConnecterDepuisPageAccueilParProfil("admin1");

        String contextKey = this.contextKeyPrefix+"_adeaffgrp";
        String contextTitle = this.contextTitlePrefix+" ADE Afficher Groupes";

        esupEmargementDriver.creerEtConfigurerNouveauContexte("admin1", contextTitle, contextKey, adminEppn, defaultSiteName, adeProjectId);
        esupEmargementDriver.configurer("ADE", "ADE_IMPORT_AFFICHER_GROUPES", true);
        // Théoriquement a faire avec un profil gestionnaire (pas besoin d'être admin)
        esupEmargementDriver.configurerAdePrefsGestionnaire(nomComposanteGestionnaire);

        String dateSession = "2026-06-29";
        String nomSession  = "Cours A1";
        String heureDebut  = "09:00";
        String heureFin    = "09:30";

        esupEmargementDriver.lancerRechercheSessionsAde(
            nomComposanteGestionnaire,
            dateSession,
            dateSession,
            nomSession
        );
        esupEmargementDriver.importerSessionAde();

        try {
            List<HashMap<String, String>> list = esupEmargementDriver.getSessions();
            assertEquals(list.size(), 1, "Le nb de résultats retournés par la recherche ADE est incorrect");
            List<HashMap<String, String>> expectedList = new ArrayList<HashMap<String, String>>();
            HashMap<String, String> session = getExpectedSessionValues(
                nomSession,
                dateSession,
                heureDebut,
                heureFin,
                "",
                "SESSION",
                1,
                1,
                0,
                null
            );
            expectedList.add(session);
            assertEquals(list, expectedList);
        } catch (Exception e) {
            assertNull(e);
        }

        HashMap<String, String> sessionDetails = esupEmargementDriver.getSessionFullDetails(0);
        HashMap<String, String> expectedSessionDetails = new HashMap<String, String>();
        expectedSessionDetails.put("Afficher groupes", "true");
        assertEquals(sessionDetails, expectedSessionDetails);
    }


    //---------------------------------------------------------------------------------
    // Context Ade 3: Configuration par défaut + sélection projet ADE
    //            + paramètre ADE_IMPORT_SOURCE_PARTICIPANTS (recherche des participants dans les groupes esup-emargement portant les même noms que les ressources - feuille - ADE) = true
    //
    // UseCase Ctx3: 
    //    - 1 ressource ADE = 1 formation et membres de la ressource non gérés dans ADE
    // UseCase Test
    //    - La resource ADE associée à l'événement est une feuille (i.e. n'est pas un dossier)     
    //---------------------------------------------------------------------------------
    @Test (dependsOnMethods={"ctxAde1ImporterSessionAde"})
    @Parameters({"admin1-eppn", "gestionnaire1-ade-composante-name", "etudiant1-eppn"})
    public void ctxAde3ImportSessionTestSourceParticipantsEsupEmargement(String adminEppn, String nomComposanteGestionnaire, String participantEppn) {
        esupEmargementDriver.seDeconnecter();
        esupEmargementDriver.seConnecterDepuisPageAccueilParProfil("admin1");

        String contextKey = this.contextKeyPrefix+"_adepartmemegrp";
        String contextTitle = this.contextTitlePrefix+" ADE Participants dans groupe EsupEmargement";

        esupEmargementDriver.creerEtConfigurerNouveauContexte("admin1", contextTitle, contextKey, adminEppn, defaultSiteName, adeProjectId);
        esupEmargementDriver.configurer("ADE", "ADE_IMPORT_SOURCE_PARTICIPANTS", "esup-emargement");
        // Théoriquement a faire avec un profil gestionnaire (pas besoin d'être admin)
        esupEmargementDriver.configurerAdePrefsGestionnaire(nomComposanteGestionnaire);

        // Initialisation du contenu du groupe
        //---------------------------------------
        String dateSession = "2026-06-29";
        esupEmargementDriver.lancerRechercheSessionsAde(nomComposanteGestionnaire, dateSession, dateSession, "Cours A1");
        esupEmargementDriver.importerSessionAde();

        // Les EPPNs doivent déjà être connus en tant que participant dans EsupEmargement
        // pour pouvoir être ajoutés ainsi dans le groupe (v1.1.3)
        esupEmargementDriver.ajouterParticipantASession(participantEppn, dateSession, "09:00", "Cours A1", null);

        ArrayList<String> eppns = new ArrayList<String>();
        eppns.add(participantEppn);

        int anneeUniv = 2025;
        String nomGroupe = "L1 A1 - Gp 1";

        esupEmargementDriver.importerParticipantsDansNouveauGroupe(nomGroupe, anneeUniv, eppns);

        // Supprimer la session qui n'a servi qu'a initialiser le groupe
        esupEmargementDriver.supprimerSession(dateSession, "09:00", "Cours A1");

        // Test proprement dit
        //--------------------------------------
        String nomSession = "Cours A2";
        dateSession = "2026-06-30";
        String heureDebut = "10:00";
        String heureFin = "10:30";
        esupEmargementDriver.lancerRechercheSessionsAde(nomComposanteGestionnaire, dateSession, dateSession, "Cours A2");
        esupEmargementDriver.importerSessionAde();

        try {
            List<HashMap<String, String>> list = esupEmargementDriver.getSessions();
            assertEquals(list.size(), 1, "Le nb de résultats retournés par la recherche ADE est incorrect");
            List<HashMap<String, String>> expectedList = new ArrayList<HashMap<String, String>>();
            HashMap<String, String> session = getExpectedSessionValues(
                nomSession,
                dateSession,
                heureDebut,
                heureFin,
                "",
                "SESSION",
                1,
                0, // Ca c'est parce que l'email de l'enseignant n'est pas renseigné (et pas dans LDAP)
                1,
                null
            );
            session.put(EsupEmargementDriver.SESSION_REPARTITION, "1 / 1");
            expectedList.add(session);
            assertEquals(list, expectedList);
        } catch (Exception e) {
            assertNull(e);
        }

        HashMap<String, String> sessionDetails = esupEmargementDriver.getSessionFullDetails(0);
        HashMap<String, String> expectedSessionDetails = new HashMap<String, String>();
        expectedSessionDetails.put("Afficher groupes", "false");
        assertEquals(sessionDetails, expectedSessionDetails);
    }

    //----------------------------------------------------------
    // UseCase Ctx3: 
    //    - 1 ressource ADE = 1 formation et membres de la ressource non gérés dans ADE
    // UseCase Test
    //    - La ressource ADE associée à l'événement ADE est un dossier
    //----------------------------------------------------------
    @Test (dependsOnMethods={"ctxAde3ImportSessionTestSourceParticipantsEsupEmargement"})
    @Parameters({"admin1-eppn", "gestionnaire1-ade-composante-name", "etudiant1-eppn", "etudiant2-eppn"})
    public void ctxAde3ImportSessionTestSourceParticipantsEsupEmargementSurRessourceGroupe(
        String adminEppn,
        String nomComposanteGestionnaire,
        String participantEppn,
        String participantEppn2
    ) {
        esupEmargementDriver.seDeconnecter();
        esupEmargementDriver.seConnecterDepuisPageAccueilParProfil("admin1");
 
        String contextKey = this.contextKeyPrefix+"_adepartmemegrp2";
        String contextTitle = this.contextTitlePrefix+" ADE Participants dans groupe EsupEmargement cas dossier";
 
        esupEmargementDriver.creerEtConfigurerNouveauContexte("admin1", contextTitle, contextKey, adminEppn, defaultSiteName, adeProjectId);
        esupEmargementDriver.configurer("ADE", "ADE_IMPORT_SOURCE_PARTICIPANTS", "esup-emargement");
        // Théoriquement a faire avec un profil gestionnaire (pas besoin d'être admin)
        esupEmargementDriver.configurerAdePrefsGestionnaire(nomComposanteGestionnaire);

        // Initialisation du contenu du groupe "L1 A1 - Gp 1"
        //---------------------------------------
        ArrayList<String> eppns = new ArrayList<String>();
        eppns.add(participantEppn);
        creerGroupeEtImporterEppns("L1 A1 - Gp 1", eppns);

        // Initialisation du contenu du groupe "L1 A1 - Gp 2"
        //---------------------------------------
        eppns = new ArrayList<String>();
        eppns.add(participantEppn2);
        creerGroupeEtImporterEppns("L1 A1 - Gp 2", eppns);

        // Test proprement dit
        //--------------------------------------
        String nomSession = "Cours A3 pour L1 A1 (Gp1 et Gp2)";
        String dateSession = "2026-06-26";
        esupEmargementDriver.lancerRechercheSessionsAde(nomComposanteGestionnaire, dateSession, dateSession, nomSession);
        esupEmargementDriver.importerSessionAde();
 
        try {
            List<HashMap<String, String>> list = esupEmargementDriver.getSessions();
            assertEquals(list.size(), 1, "Le nb de résultats retournés par la recherche ADE est incorrect");
            List<HashMap<String, String>> expectedList = new ArrayList<HashMap<String, String>>();
            HashMap<String, String> session = getExpectedSessionValues(
                nomSession,
                dateSession,
                "09:00",
                "09:15",
                "",
                "SESSION",
                1,
                1,
                2,
                null
            );
            
            expectedList.add(session);
            assertEquals(list, expectedList);
        } catch (Exception e) {
            assertNull(e);
        }
 
        HashMap<String, String> sessionDetails = esupEmargementDriver.getSessionFullDetails(0);
        HashMap<String, String> expectedSessionDetails = new HashMap<String, String>();
        expectedSessionDetails.put("Afficher groupes", "false");
        assertEquals(sessionDetails, expectedSessionDetails);
    }

    //------------------------
    // Toolkit
    //------------------------
    protected void creerEtInitialiserNouveauContexte(String contextKey, String contextTitle, String adminEppn, String defaultSiteName)
    {
        esupEmargementDriver.creerNouveauContexte(contextTitle, contextKey);
        esupEmargementDriver.ajouterAdministrateurContexte(contextKey, adminEppn);
        // Pour être "sûr" de voir le nouveau contexte, de déconnecter/reconnecter
        esupEmargementDriver.seDeconnecter();
        // FIXME Choisir entre passer en paramètre adminEppn et utiliser profileId "admin1"
        esupEmargementDriver.seConnecterDepuisPageAccueilParProfil("admin1");
        esupEmargementDriver.selectionnerContexte(contextKey);

        esupEmargementDriver.ajouterSite(defaultSiteName, true);
    }

    protected void creerGroupeEtImporterEppns(
        String nomGroupe,
        ArrayList<String> eppns
    ) {
        int anneeUniv = 2025;
        String nomSessionFictive = "Session fictive pour peuplement groupe";
        String dateSessionFictive = "2026-06-01";
        String heureDebutSessionFictive = "09:00";
        String heureFinSessionFictive = "10:00";

        // Initialisation du contenu du groupe "L1 A1 - Gp 1"
        //---------------------------------------
        esupEmargementDriver.creerSession(nomSessionFictive, dateSessionFictive, heureDebutSessionFictive, heureFinSessionFictive);
 
        // Les EPPNs doivent déjà être connus en tant que participant dans EsupEmargement
        // pour pouvoir être ajoutés ainsi dans le groupe (v1.1.3)
        for (String participantEppn: eppns) {
            esupEmargementDriver.ajouterParticipantASession(participantEppn, dateSessionFictive, heureDebutSessionFictive, nomSessionFictive, null);
        }
 
        esupEmargementDriver.importerParticipantsDansNouveauGroupe(nomGroupe, anneeUniv, eppns);
 
         // Supprimer la session qui n'a servi qu'a initialiser le groupe
        esupEmargementDriver.supprimerSession(dateSessionFictive, heureDebutSessionFictive, nomSessionFictive);
    }


    protected HashMap<String, String> getExpectedSessionValues(
        String nom,
        String date,
        String heureDebut,
        String heureFin,
        String typeSession,
        String typeBadgeage,
        int lieuCount,
        int surveillantCount,
        int participantCount,
        String groupes
    ) {
        HashMap<String, String> session = new HashMap<String, String>();
        session.put(EsupEmargementDriver.SESSION_DATE, EsupEmargementDriver.getDateDDMMYYYYFromYYYYMMDD(date));
        session.put(EsupEmargementDriver.SESSION_TYPE, typeSession);
        session.put(EsupEmargementDriver.SESSION_NOM, nom);
        if (null != groupes) {
            session.put(EsupEmargementDriver.SESSION_GROUPE,groupes);
        }
        session.put(EsupEmargementDriver.SESSION_STATUT, "En attente");
        session.put(EsupEmargementDriver.SESSION_BADGEAGE, typeBadgeage);
        session.put(EsupEmargementDriver.SESSION_LIBRE, "");
        session.put(EsupEmargementDriver.SESSION_ADE, "");
        session.put(EsupEmargementDriver.SESSION_LIEUX, ""+lieuCount);
        session.put(EsupEmargementDriver.SESSION_SURVEILLANTS, ""+surveillantCount);
        session.put(EsupEmargementDriver.SESSION_INSCRITS, ""+participantCount);
        session.put(EsupEmargementDriver.SESSION_SITE, defaultSiteName);
        session.put(EsupEmargementDriver.SESSION_PJ, "0");
        session.put(EsupEmargementDriver.SESSION_CONVOCATION, getHeureConvocationParDefaut(date, heureDebut));
        session.put(EsupEmargementDriver.SESSION_DEBUT, heureDebut);
        session.put(EsupEmargementDriver.SESSION_FIN, heureFin);
        session.put(EsupEmargementDriver.SESSION_DUREE, getDureeEpreuve(date, heureDebut, heureFin));
        session.put(EsupEmargementDriver.SESSION_TEMPS, "");
        session.put(EsupEmargementDriver.SESSION_REPARTITION, "0 / "+participantCount);
        session.put(EsupEmargementDriver.SESSION_PRESENCE, "0 / "+participantCount);

        return session;
    }

    public String getHeureConvocationParDefaut(String dateSession, String heureDebut) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        DateTimeFormatter dfHour = DateTimeFormatter.ofPattern("HH:mm");

        return LocalDateTime.parse(dateSession+" "+heureDebut, df).minusMinutes(15).format(dfHour);
    }

    public String getDureeEpreuve(String dateSession, String heureDebut, String heureFin) {
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");

            Date heureEpreuve = df.parse(dateSession+" "+heureDebut);
            Date finEpreuve = df.parse(dateSession+" "+heureFin);

            String duree ="";
            long diff = finEpreuve.getTime() - heureEpreuve.getTime();

            long diffMinutes = diff / (60 * 1000) % 60;
            long diffHours = diff / (60 * 60 * 1000) % 24;

            if (diffHours != 0) {
                duree = String.valueOf(diffHours).concat("H");
            }

            if (diffMinutes != 0) {
                duree = duree.concat(StringUtils.leftPad(String.valueOf(diffMinutes), 2, "0"));
                if (diffHours == 0) {
                    duree = duree.concat("mn");
                }
            }

            return duree;
        } catch (ParseException e) {
            System.out.println(e);
            return null;
        }
    }
}