import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

public class EsupEmargementDriver {

    static final String SESSION_ADE = "ADE";
    static final String SESSION_BADGEAGE = "Badgeage";
    static final String SESSION_CONVOCATION = "Convocation";
    static final String SESSION_DATE = "Date";
    static final String SESSION_DEBUT = "Début";
    static final String SESSION_DUREE = "Durée";
    static final String SESSION_ENSEIGNANTS = "Enseignants";
    static final String SESSION_FIN = "Fin";
    static final String SESSION_FORMATION = "Formation";
    static final String SESSION_GROUPE = "Groupe";
    static final String SESSION_INSCRITS = "Inscrits";
    static final String SESSION_LIBRE = "Libre";
    static final String SESSION_LIEUX = "Lieux";
    static final String SESSION_NOM = "Nom";
    static final String SESSION_PJ = "PJ";
    static final String SESSION_PRESENCE = "Présence";
    static final String SESSION_REPARTITION = "Répartition";
    static final String SESSION_SALLES = "Salles";
    static final String SESSION_SITE = "Site";
    static final String SESSION_STATUT = "Statut";
    static final String SESSION_SURVEILLANTS = "Surveillants";
    static final String SESSION_TEMPS = "Temps";
    static final String SESSION_TYPE = "Type";

    static final String GROUPE_ANNEE = "Année";
    static final String GROUPE_DESCRIPTION = "Description";
    static final String GROUPE_MEMBRES = "Membres";
    static final String GROUPE_MODIFICATEUR = "Modificateur";
    static final String GROUPE_NOM = "Nom";

    private WebDriver webDriver;
    private CasDriver casDriver;
    private String url = null;
    private String esupEmargementVersion;

    boolean isScreenshotEnabled = false;
    int     screenshotCounter   = 0;

    public EsupEmargementDriver(WebDriver driver, String url) {
        this.webDriver = driver;
        this.webDriver.get(url);
        this.url = url;
    }

    public EsupEmargementDriver(CasDriver casDriver, String url) {
        this.casDriver = casDriver;
        this.webDriver = casDriver.getWebDriver();
        this.webDriver.get(url);
        this.url = url;
    }

    public void accederEsupEmargement()
    {
        this.webDriver.get(url);
    }

    public void quitterEsupEmargement() {
        webDriver.close();
    }

    public String getTitle() {
        return webDriver.getTitle();
    }

    public void accederPageDeConnexionDepuisPageAccueil() {
        webDriver.findElement(By.linkText("Connexion")).click();
    }

    public void seConnecterDepuisPageCas(String login, String password) {
        casDriver.fillFormAndSubmit(login, password);

        takeFullPageScreenshot("seConnecterAdminDepuisPageCas");
    }

    public void seConnecterDepuisPageAccueil(String login, String password) {
        accederPageDeConnexionDepuisPageAccueil();
        seConnecterDepuisPageCas(login, password);
    }

    public void seConnecterDepuisPageCasParProfil(String profileId) {
        casDriver.fillFormAndSubmitUsingProfile(profileId);

        takeFullPageScreenshot("seConnecterDepuisPageCasParProfil");
    }

    public void seConnecterDepuisPageAccueilParProfil(String profileId) {
        accederPageDeConnexionDepuisPageAccueil();
        seConnecterDepuisPageCasParProfil(profileId);
    }

    public void seDeconnecter() {
        seDeconnecter(true);
    }

    public void seDeconnecter(boolean revenirPageAccueil) {
        webDriver.findElement(By.linkText("Déconnexion")).click();
        if (revenirPageAccueil) {
            this.webDriver.get(url);
        } // Sinon on reste sur la page de sortie CAS
    }

    protected String getEsupEmargementVersion()
    {
        if (null == esupEmargementVersion) {
            // REM: On supprime le "v" de vX.X.X
            esupEmargementVersion = webDriver.findElement(By.xpath("//footer/span[2]")).getText().substring(1);
        }

        return esupEmargementVersion;
    }

    public void selectionnerContexte(String context) {
        Select dropDown = new Select(webDriver.findElement(By.id("selectContext")));
        dropDown.selectByVisibleText(context);
    }

    public void creerNouveauContexte(String contextTitle, String contextKey) {
        creerNouveauContexte(contextTitle, contextKey, true);
    }

    // Penser à lui ajouter un administrateur
    public void creerNouveauContexte(String contextTitle, String contextKey, boolean isActif) {
        selectionnerContexte("all");
        webDriver.findElement(By.linkText("Contextes")).click();
        webDriver.findElement(By.cssSelector("a[title='Ajouter un contexte']")).click();
        webDriver.findElement(By.id("key")).sendKeys(contextKey);
        webDriver.findElement(By.id("title")).sendKeys(contextTitle);
        if (isActif) {
            webDriver.findElement(By.id("isActif1")).click();
        }
        webDriver.findElement(By.cssSelector("input[value='Valider']")).click();
    }

    // TODO Eviter d'avoir a passer profileId ET adminEppn
    public void creerEtConfigurerNouveauContexte(String profileId, String contextTitle, String contextKey, String adminEppn, String defaultSiteName, int adeProjectId) {
        creerNouveauContexte(contextTitle, contextKey);
        ajouterAdministrateurContexte(contextKey, adminEppn);

        // se déconnecter/reconnecter pour être sûr d'avoir le nouveau contexte
        // dans la liste déroulane
        seDeconnecter();
        seConnecterDepuisPageAccueilParProfil(profileId);

        selectionnerContexte(contextKey);
        configurerAdeProjetParDefaut(adeProjectId);
        ajouterSite(defaultSiteName, true);
    }

    public void supprimerContextesAvecTitreCommencantPar(String debutTitre) throws Exception
    {
        selectionnerContexte("all");
        webDriver.findElement(By.linkText("Contextes")).click();

        String xpath = "//div[@class='card-header']//h2[text()='Contextes']/ancestor::div[@class='card']/div[@class='card-body']//tbody/tr/td[2][starts-with(text(), '"+debutTitre+"')]/..";

        List<WebElement> contextLineElements = webDriver.findElements(By.xpath(xpath));

        while (contextLineElements.size() > 0) {
            WebElement contextElement = contextLineElements.get(0);
            String contextId = contextElement.findElement(By.xpath("td[1]")).getText();

            contextElement.findElement(By.xpath("td//i[contains(@class, 'fa-trash')]")).click();
            takeFullPageScreenshot("supprimerContextesAvecTitreCommencantPar.clickSuppression");

            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(5));

            String xpathConfirmModalBody = "//div[@class='modal-body']/div[starts-with(text(), 'Confirmez-vous la suppression de ce contexte')]/span[text()='"+contextId+"']/../..";
            String xpathConfirmButton = xpathConfirmModalBody+"/../div[@class='modal-footer']/button[text()='Valider']";
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpathConfirmButton)));
            //takeFullPageScreenshot("popupConfirmation");
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpathConfirmButton)));

            String xpathConfirmYesRadio = xpathConfirmModalBody+"//label[text()='Oui']/../input";
            webDriver.findElement(By.xpath(xpathConfirmYesRadio)).click();
            takeFullPageScreenshot("supprimerContextesAvecTitreCommencantPar.confirmeSuppression");

            webDriver.findElement(By.xpath(xpathConfirmButton)).click();
            takeFullPageScreenshot("supprimerContextesAvecTitreCommencantPar.suppressionConfirmee");

            // Find by context key
            List<WebElement> deletedLine = webDriver.findElements(By.xpath(xpath+"/td[1][text()='"+contextId+"']"));
            if (deletedLine.size() > 0) {
                throw new Exception("La suppression du contexte [" +contextId+"] ne semble pas avoir fonctionné");
            }

            contextLineElements = webDriver.findElements(By.xpath(xpath));
        }
    }

    public void ajouterAdministrateurContexte(String contextKey, String eppn) {
        webDriver.findElement(By.linkText("Agents")).click();
        webDriver.findElement(By.linkText(contextKey)).click();
        // Afficher, dans les onglets, le nom du contexte plutôt que la clé aurait été plus sympa
        // (mais prend potentiellement plus de place)
        webDriver.findElement(By.cssSelector("a[title='Ajouter un administrateur']")).click();

        // FIXME On a beau sélectionner un contexte, lorsque l'on clique sur Ajouter un administrateur
        // le contexte par défaut n'est pas celui précédemment sélectionné
        // ATTENTION: Une fois le point saisi, il n'y a plus d'auto-complétion (v1.1.3, v1.1.4-SNAPSHOT)
        // Ici, on suppose que le point est au delà du 5ème caractère.
        if ("1.1.3".equals(getEsupEmargementVersion())) {
            webDriver.findElement(By.id("eppn")).sendKeys(eppn.substring(0, 5));
        } else { // > 1.1.3
            webDriver.findElement(By.cssSelector("input[type='email']")).sendKeys(eppn.substring(0, 5));
        }

        // Attendre l'affichage de la liste de choix des individus
        // TODO Ne pas supposer que le premier résultat est le bon mais vérifier dans la liste
        // Pas sûr que cela fonctionne à tous les coups (because id autocomplete ? à voir)
        String autoCompleteLineId = "awesomplete_list_1_item_0";

        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(5));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id(autoCompleteLineId)));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(autoCompleteLineId)));

        // Sélectionner l'individu dans la liste
        webDriver.findElement(By.id(autoCompleteLineId)).click();

        // ... son rôle
        Select dropDown = new Select(webDriver.findElement(By.id("userRole")));
        dropDown.selectByVisibleText("ADMINISTRATEUR");

        // ... pour le contexte
        dropDown = new Select(webDriver.findElement(By.id("context.key")));
        dropDown.selectByVisibleText(contextKey);
        // Afficher, dans la liste de sélection, le nom du contexte plutôt que la clé aurait été plus sympa

        webDriver.findElement(By.cssSelector("input[value='Valider']")).click();
    }

    public void accederOngletConfiguration(String tab) {
        webDriver.findElement(By.linkText("Administrateur")).click();
        webDriver.findElement(By.linkText("Configurations")).click();
        webDriver.findElement(By.linkText(tab)).click();

        takeFullPageScreenshot("accederOngletConfiguration");
    }

    public void configurer(String tab, String key, String value) {
        accederOngletConfiguration(tab);

        WebElement trNode = webDriver.findElement(By.xpath("//td[text()='"+key+"']/../."));
        trNode.findElement(By.cssSelector("i[class~='fa-pen']")).click();

        webDriver.findElement(By.id("valeur")).clear();
        webDriver.findElement(By.id("valeur")).sendKeys(value);
        webDriver.findElement(By.cssSelector("input[value='Valider']")).click();
    }

    public void configurer(String tab, String key, boolean value) {
        accederOngletConfiguration(tab);
        WebElement trNode = webDriver.findElement(By.xpath("//td[text()='"+key+"']/../."));
        trNode.findElement(By.cssSelector("i[class~='fa-pen']")).click();
        takeFullPageScreenshot();

        webDriver.findElement(By.id("bool"+(value?"True":"False"))).click();
        takeFullPageScreenshot();

        webDriver.findElement(By.cssSelector("input[value='Valider']")).click();
    }

    public void configurerAdeProjetParDefaut(int projectId) {
        configurer("ADE", "ADE_PROJET", ""+projectId);
    }

    public void ajouterSite(String siteName) {
        ajouterSite(siteName, false);
    }

    public void ajouterSite(String siteName, boolean isDefault) {
        webDriver.findElement(By.linkText("Administrateur")).click();
        webDriver.findElement(By.linkText("Sites")).click();

        // FIXME title du bouton erroné dans l'application. Devrait être "Ajouter un site"
        webDriver.findElement(By.cssSelector("a[title='Ajouter un lieu']")).click();

        webDriver.findElement(By.id("site")).sendKeys(siteName);

        if (isDefault) {
            webDriver.findElement(By.id("isDefault1")).sendKeys(siteName);
        }

        webDriver.findElement(By.cssSelector("input[value='Valider']")).click();
    }

    public void accederEcranGestionnaireSessions() {
        webDriver.findElement(By.linkText("Gestionnaire")).click();
        //takeFullPageScreenshot();

        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(5));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.linkText("Sessions")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Sessions")));

        webDriver.findElement(By.linkText("Sessions")).click();
    }

    public void accederEcranGestionnaireAdeCampus() {
        webDriver.findElement(By.linkText("Gestionnaire")).click();
        //takeFullPageScreenshot();

        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(5));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.linkText("Ade Campus")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.linkText("Ade Campus")));

        webDriver.findElement(By.linkText("Ade Campus")).click();
    }

    public void creerGroupe(String nomGroupe, int anneeUniv) {
        webDriver.findElement(By.linkText("Gestionnaire")).click();
        webDriver.findElement(By.linkText("Groupes")).click();

        webDriver.findElement(By.cssSelector("a[title='Ajouter un groupe']")).click();

        webDriver.findElement(By.id("nom")).sendKeys(nomGroupe);
        webDriver.findElement(By.id("description")).sendKeys(nomGroupe);
        webDriver.findElement(By.id("anneeUniv")).sendKeys(""+anneeUniv);
        webDriver.findElement(By.cssSelector("input[value='Valider']")).click();
    }

    // REM: v1.1.5 Pour pouvoir être ajoutés au groupe, les participants doivent précédememnt avoir été ajoutés
    //      à esup-émargement en tant que participant (par exemple en ayant été ajouté manuellement à une session)
    public void importerParticipantsDansGroupe(String nomGroupe, List<String> eppns) {
        webDriver.findElement(By.linkText("Gestionnaire")).click();
        webDriver.findElement(By.linkText("Groupes")).click();

        int nbMembers = 0;
        for (String eppn: eppns) {
            webDriver.findElement(By.cssSelector("a[title='Ajouter des membres']")).click();

            webDriver.findElement(By.xpath("//label[text()='Importer dans groupe(s)']/../div[contains(@class, 'ss-main')]")).click();
            // Code avant 1.1.5-SNAPSHOT
            //webDriver.findElement(By.xpath("//label[text()='Importer dans groupe(s)']/../div[contains(@class, 'ss-main')]//div[@class='ss-option'][text()='"+nomGroupe+" (0)']")).click();
            webDriver.findElement(By.xpath("//div[@class='ss-option'][text()='"+nomGroupe+" ("+nbMembers+")']")).click();
            takeFullPageScreenshot("creerGroupeParticipants.groupeSelectionne");

            // ATTENTION Si l'on ne passe pas par la sélection dans la liste, la saisie de l'EPPN doit évidemment être
            // complète et correcte mais elle doit également respecter la casse (v1.1.3)
            if ("1.1.3".equals(getEsupEmargementVersion())) {
                webDriver.findElement(By.xpath("//label[text()='Eppn']/..//input[@name='eppnTagCheck']")).sendKeys(eppn);
            } else {
                webDriver.findElement(By.xpath("//label[text()='Eppn']/..//input[@name='searchString']")).sendKeys(eppn);
            }

            takeFullPageScreenshot("creerGroupeParticipants.eppnSaisi");
            webDriver.findElement(By.xpath("//button[text()='Ajouter']")).click();

            nbMembers++;
            // Après ajout on revient automatiquement à la liste des groupes
        }
    }

    public void importerParticipantsDansNouveauGroupe(String nomGroupe, int anneeUniv, List<String> eppns) {
        creerGroupe(nomGroupe, anneeUniv);
        importerParticipantsDansGroupe(nomGroupe, eppns);
    }

    protected List<HashMap<String, String>> getGroupes() throws Exception {
        webDriver.findElement(By.linkText("Gestionnaire")).click();
        webDriver.findElement(By.linkText("Groupes")).click();

        takeFullPageScreenshot();

        String xpath = "//th[text()='Membres']/ancestor::table/tbody/tr";

        List<WebElement> composantesElements = webDriver.findElements(By.xpath(xpath));
        ArrayList<String> idx2FieldName = new ArrayList<String>();
        idx2FieldName.add(GROUPE_NOM);
        idx2FieldName.add(GROUPE_DESCRIPTION);
        idx2FieldName.add(GROUPE_MEMBRES);
        idx2FieldName.add(null); // Date de création: Ne sera probablement pas testé
        idx2FieldName.add(null); // Date de modification: Ne sera probablement pas testé
        idx2FieldName.add(GROUPE_MODIFICATEUR);
        idx2FieldName.add(GROUPE_ANNEE);
        idx2FieldName.add(null); // Actions

        return getListFromTableLineElements(composantesElements, idx2FieldName, "Groupes");
    }


    protected List<HashMap<String, String>> getListFromTableLineElements(List<WebElement> lineElements, ArrayList<String> idx2FieldName, String tableDescription) throws Exception{
        List<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
        for (WebElement lineElement : lineElements) {
            HashMap<String, String> sessionAde = new HashMap<String, String>();
            List<WebElement> cellElements = lineElement.findElements(By.xpath("td"));
            int colNb = 0;
            for (WebElement cellElement: cellElements) {
                if (colNb >= idx2FieldName.size()) {
                    throw new Exception("Nb de colonnes trouvées dans la table "+tableDescription+" supérieur au nb attendu ("+cellElements.size()+" vs "+idx2FieldName.size()+")");
                }

                String fieldName = idx2FieldName.get(colNb);
                if (null != fieldName) {
                    sessionAde.put(fieldName, cellElement.getText());
                }

                colNb++;
            }

            list.add(sessionAde);
        }

        return list;
    }

    public List<HashMap<String, String>> getAdePrefsComposantes() throws Exception {
        accederEcranGestionnaireAdeCampusPrefs();
        if ("1.1.3".equals(getEsupEmargementVersion())) {
            // ATTENTION L'id de la table "Salles" est erroné et est identique à celui de la table "Composantes"
            //return getAdePrefsByTableId("composantesTable");
            return getAdePrefsByXPathDepuisEcranAdeCampusPrefs("//caption/span[text()='Composantes']/../../tbody/tr", "Composantes");
        } else { // > 1.1.3
            return getAdePrefsByXPathDepuisEcranAdeCampusPrefs("//caption[text()='Composantes']/../tbody/tr", "Composantes");
        }
    }

    public List<HashMap<String, String>> getAdePrefsFormations() throws Exception {
        accederEcranGestionnaireAdeCampusPrefs();
        return getAdePrefsByTableIdDepuisEcranAdeCampusPrefs("formationsTable", "Formations");
    }

    public List<HashMap<String, String>> getAdePrefsSalles() throws Exception {
        accederEcranGestionnaireAdeCampusPrefs();
        if ("1.1.3".equals(getEsupEmargementVersion())) {
            // ATTENTION L'id de la table "Salles" est erroné et est identique à celui de la table "Composantes"
            return getAdePrefsByXPathDepuisEcranAdeCampusPrefs("//caption/span[text()='Salles']/../../tbody/tr", "Salles");
        } else { // > 1.1.3
            return getAdePrefsByXPathDepuisEcranAdeCampusPrefs("//caption[text()='Salles']/../tbody/tr", "Salles");
        }
    }

    protected List<HashMap<String, String>> getAdePrefsByTableIdDepuisEcranAdeCampusPrefs(String tableId, String tableDescription) throws Exception {
        webDriver.findElement(By.cssSelector("i[class~='fa-gear']")).click();
        return getAdePrefsByXPathDepuisEcranAdeCampusPrefs("table[@id='"+tableId+"']/tbody/tr", tableDescription);
    }

    protected List<HashMap<String, String>> getAdePrefsByXPathDepuisEcranAdeCampusPrefs(String xpath, String tableDescription) throws Exception {
        List<WebElement> composantesElements = webDriver.findElements(By.xpath(xpath));
        ArrayList<String> idx2FieldName = new ArrayList<String>();
        idx2FieldName.add(null); // case à cocher
        // colonne cachée dans la table "Composantes". Une raison à cela ?
        if (tableDescription.equals("Composantes")) {
            idx2FieldName.add(null);
        }
        idx2FieldName.add("Libellé");

        return getListFromTableLineElements(composantesElements, idx2FieldName, tableDescription);
    }

    public void accederEcranGestionnaireAdeCampusPrefs() {
        accederEcranGestionnaireAdeCampus();
        if ("1.1.3".equals(getEsupEmargementVersion())) {
            webDriver.findElement(By.cssSelector("i[class~='fa-gear']")).click();
        } else { // > 1.1.3
            webDriver.findElement(By.cssSelector("i[class~='fa-tasks']")).click();
            webDriver.findElement(By.linkText("Paramètres")).click();
        }
    }

    public void configurerAdePrefsGestionnaire(String composante) {
        accederEcranGestionnaireAdeCampusPrefs();

        WebElement composantesElements = webDriver.findElement(By.id("composantesTable"));
        composantesElements.findElement(By.xpath("//td[text()='"+composante+"']/..//input[@type='checkbox']")).click();

        takeFullPageScreenshot("configurerAdePrefsGestionnaire");

        // FIXME TYPO Enregist(r)er
        composantesElements.findElement(By.xpath("//button[text()='Enregister']")).click();

        if ("1.1.3".equals(getEsupEmargementVersion())) {
            // Pas de pop-up de confirmation
        } else { // > 1.1.3
            Alert alert = webDriver.switchTo().alert();
            alert.accept();
            // marquer une pause ??
            // Les tests échouent souvent par la suite au moment de cliquer sur "Gestionnaire" dans le menu
            // ou sur "Ade Campus"
            // Est-ce que l'on clique avant que la page ne soit rafraichie ?
            try {
                java.lang.Thread.sleep(1000);
            } catch (InterruptedException e) {
                // et alors ?
            }
        }

        takeFullPageScreenshot("configurerAdePrefsGestionnaire.fin");
    }

    // TODO Permettre de choisir les champs à retourner
    public List<HashMap<String, String>> getListeSessionsAdeDepuisEcranResultatRecherche() throws Exception {
        WebElement tableElement = webDriver.findElement(By.id("tableEvents"));
        List<WebElement> sessionsAdeElements = tableElement.findElements(By.xpath("tr"));

        ArrayList<String> idx2FieldName = new ArrayList<String>();
        idx2FieldName.add(null); // 0 - ids (TODO A traiter ?)
        idx2FieldName.add(null); // 1 - case à cocher
        idx2FieldName.add(null); // 2 - ???
        idx2FieldName.add(null); // 3 - ???
        idx2FieldName.add(null); // 4 - ???
        idx2FieldName.add(null); // 5 - ???
        idx2FieldName.add(null); // 6 - ???
        idx2FieldName.add(null); // 7 - ???
        idx2FieldName.add(null); // 8 - Maj
        idx2FieldName.add(null); // 9 - Import
        idx2FieldName.add(SESSION_TYPE);
        idx2FieldName.add(SESSION_NOM);
        idx2FieldName.add(SESSION_DATE);
        idx2FieldName.add(SESSION_DEBUT);
        idx2FieldName.add(SESSION_FIN);
        idx2FieldName.add(SESSION_GROUPE);
        idx2FieldName.add(SESSION_ENSEIGNANTS);
        idx2FieldName.add(SESSION_SALLES);
        idx2FieldName.add(SESSION_FORMATION);


        return getListFromTableLineElements(sessionsAdeElements, idx2FieldName, "Sessions ADE");
    }

    public void creerSession(String nomSession, String dateSession, String heureDebut, String heureFin)
    {
        accederEcranGestionnaireSessions();

        // Cliquer sur bouton '+'
        webDriver.findElement(By.xpath("//div[@id='sessionSearch']//i[contains(@class,'fa-plus')]")).click();

        // Remplir formulaire de nouvelle session

        // Type: Prendre une valeur quelconque
        String type = "Examen";
        Select dropDown = new Select(webDriver.findElement(By.id("typeSession")));
        dropDown.selectByVisibleText(type);

        // Site: Supposé pré-rempli

        // Nom
        webDriver.findElement(By.id("nomSessionEpreuve")).clear();
        webDriver.findElement(By.id("nomSessionEpreuve")).sendKeys(nomSession);

        // Date
        webDriver.findElement(By.id("dateSessionEpreuve")).clear();
        webDriver.findElement(By.id("dateSessionEpreuve")).sendKeys(dateSession);

        // Heure de début
        webDriver.findElement(By.id("heureEpreuve")).clear();
        webDriver.findElement(By.id("heureEpreuve")).sendKeys(heureDebut);

        // Heure de fin
        webDriver.findElement(By.id("finEpreuve")).clear();
        webDriver.findElement(By.id("finEpreuve")).sendKeys(heureFin);

        takeFullPageScreenshot("creerSession.saisieComplete");

        webDriver.findElement(By.cssSelector("input[value='Valider']")).click();
        takeFullPageScreenshot("creerSession.valide");
    }

    // FIXME Gérer le cas où l'url ADE est fausse (actuellement affiche une page d'exception non catchée)
    // FIXME Alerter si aucun site n'a été sélectionné (i.e. si aucun site n'a été préalablement créé)
    public void lancerRechercheSessionsAde(String composante, String dateDebut, String dateFin, String nomSessionAttendue) {
        accederEcranGestionnaireAdeCampus();

        // Sélection de la branche ADE
        Select dropDown = new Select(webDriver.findElement(By.id("codeComposante1")));
        dropDown.selectByVisibleText(composante);
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(5));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("frmt")));
        String treeNodeToSelectXPath = "//div[@id='frmt']//a[text()='"+composante+"']";
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(treeNodeToSelectXPath)));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(treeNodeToSelectXPath)));
        webDriver.findElement(By.xpath(treeNodeToSelectXPath)).click();

        // Saisie des paramètres de recherche
        webDriver.findElement(By.id("strDateMin")).clear();
        webDriver.findElement(By.id("strDateMin")).sendKeys(dateDebut);
        webDriver.findElement(By.id("strDateMax")).clear();
        webDriver.findElement(By.id("strDateMax")).sendKeys(dateFin);

        // Lancer la recherche
        webDriver.findElement(By.xpath("//input[@value='Rechercher']")).click();

        takeFullPageScreenshot("lancerRechercheSessionsAde.resultatRecherche");

        // Attendre une réponse. Comment distinguer réponse pas encore arrivée vs réponse vide ?
        // Du coup, on est obligé de préciser le nom de la session que l'on attend
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//table[@id='grid']//td[text()='"+nomSessionAttendue+"']")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//table[@id='grid']//td[text()='"+nomSessionAttendue+"']")));

        takeFullPageScreenshot("lancerRechercheSessionsAde.reponse");
    }

    // TODO Préciser quel session importer. En attendant, on importe la première de la liste
    public void importerSessionAde() {
        // Sélection de la session
        WebElement tableElement = webDriver.findElement(By.id("tableEvents"));
        List<WebElement> lineElements = tableElement.findElements(By.xpath("tr"));
        WebElement lineElement = lineElements.get(0);
        lineElement.findElement(By.cssSelector("input[name='btSelectItem']")).click();

        // Lancer import
        webDriver.findElement(By.xpath("//button[text()='Importer']")).click();
    }

    public List<HashMap<String, String>> getSessions() throws Exception {
        takeFullPageScreenshot("getSessions.beforeAccess");
        accederEcranGestionnaireSessions();

        takeFullPageScreenshot("getSessions");

        List<WebElement> sessionsElements = webDriver.findElements(By.xpath("//table[@id='tableSessionEpreuve']/tbody/tr"));

        List<WebElement> headElements = webDriver.findElements(By.xpath("//table[@id='tableSessionEpreuve']/thead/tr/th"));
            
        ArrayList<String> idx2FieldName = new ArrayList<String>();
        idx2FieldName.add(null); // 0 - case à cocher
        idx2FieldName.add(SESSION_DATE); // 1
        idx2FieldName.add(SESSION_TYPE); // 2
        idx2FieldName.add(SESSION_NOM); // 3
        if ("Groupes".equals(headElements.get(4).getText())) {
            idx2FieldName.add(SESSION_GROUPE); // Si affichage dans groupe liste session activé (évol post 1.1.5)
        }
        idx2FieldName.add(SESSION_STATUT);
        idx2FieldName.add(SESSION_BADGEAGE);
        idx2FieldName.add(SESSION_LIBRE);
        idx2FieldName.add(SESSION_ADE);
        idx2FieldName.add(SESSION_LIEUX);
        idx2FieldName.add(SESSION_SURVEILLANTS);
        idx2FieldName.add(SESSION_INSCRITS);
        idx2FieldName.add(SESSION_SITE);
        idx2FieldName.add(SESSION_PJ);
        idx2FieldName.add(SESSION_CONVOCATION);
        idx2FieldName.add(SESSION_DEBUT);
        idx2FieldName.add(SESSION_FIN);
        idx2FieldName.add(SESSION_DUREE);
        idx2FieldName.add(SESSION_TEMPS);
        idx2FieldName.add(SESSION_REPARTITION);
        idx2FieldName.add(SESSION_PRESENCE);
        idx2FieldName.add(null); // Actions

        return getListFromTableLineElements(sessionsElements, idx2FieldName, "Sessions");
    }

    public WebElement getSessionLineWebElement(String date, String heureDebut) {
        return getSessionLineWebElement(date, heureDebut, null);
    }

    /**
     * 
     * @param date YYYY-MM-DD
     * @param heureDebut
     * @param nom
     * @return
     */
    public WebElement getSessionLineWebElement(String date, String heureDebut, String nom) {
        String dateDDMMYYYY = getDateDDMMYYYYFromYYYYMMDD(date);

        String heureDebutColName = "Début";
        String heureDebutColIdx = "position()=count(//table[@id='tableSessionEpreuve']/thead/tr//th[text()='"+heureDebutColName+"']/preceding-sibling::th)+1";
        String dateColIdx = "2";
        String xpath = "//table[@id='tableSessionEpreuve']/tbody/tr/td["+dateColIdx+"]/span[text()='"+dateDDMMYYYY+"']/../.."+
            "/td["+heureDebutColIdx+"][text()='"+heureDebut+"']";

        webDriver.findElement(By.xpath(xpath));

        if (null != nom) {
            xpath += "/../td[4][text()='"+nom+"']";
        }

        xpath += "/..";
        return webDriver.findElement(By.xpath(xpath));
    }

    public HashMap<String, String> getSessionFullDetails(int sessionIdx) {
        accederEcranGestionnaireSessions();

        // Il faut être en mode edition pour voir tous les champs
        webDriver.findElement(By.xpath("//table[@id='tableSessionEpreuve']/tbody/tr["+(sessionIdx+1)+"]//i[contains(@class,'fa-pen')]")).click();

        HashMap<String, String> fullDetails = new HashMap<String, String>();

        WebElement checkBoxElement = webDriver.findElement(By.id("isGroupeDisplayed1"));
        if (null != checkBoxElement.getAttribute("checked")) {
            fullDetails.put("Afficher groupes", "true");
        } else {
            fullDetails.put("Afficher groupes", "false");
        }

        return fullDetails;
    }

    public void supprimerSession(String date, String heureDebut) {
        supprimerSession(date, heureDebut, null);
    }

    public void supprimerSession(String date, String heureDebut, String nom) {
        accederEcranGestionnaireSessions();

        takeFullPageScreenshot("supprimerSession.accueil");

        WebElement sessionLine = getSessionLineWebElement(date, heureDebut, nom);
        sessionLine.findElement(By.cssSelector("i[class~='fa-trash']")).click();


        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(5));

        String xpathConfirmStart = "//div[@class='modal-body']/div[starts-with(text(), 'Confirmez-vous la suppression de cette session')]";
        String xpathConfirmPopup = xpathConfirmStart+"/../../div[@class='modal-footer']/button[text()='Valider']";
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpathConfirmPopup)));
        //takeFullPageScreenshot("popupConfirmation");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpathConfirmPopup)));

        webDriver.findElement(By.xpath(xpathConfirmPopup)).click();
    }

    public void ajouterParticipantASession(String eppn, String date, String heureDebut) {
        ajouterParticipantASession(eppn, date, heureDebut, null, null);
    }

    public void ajouterParticipantASession(String eppn, String date, String heureDebut, String nom, String lieu) {
        accederEcranGestionnaireSessions();
        takeFullPageScreenshot("ajouterParticipantASession.accueil");

        WebElement sessionLine = getSessionLineWebElement(date, heureDebut, nom);
        // Click sur le compteur d'inscrits
        String nbInscritsColName = "Inscrits";
        String nbInscritsColIdx = "position()=count(//table[@id='tableSessionEpreuve']/thead/tr//th[text()='"+nbInscritsColName+"']/preceding-sibling::th)+1";

        sessionLine.findElement(By.xpath("td["+nbInscritsColIdx+"]/a")).click();
        takeFullPageScreenshot("ajouterParticipantASession.postClickNbInscrits");

        webDriver.findElement(By.cssSelector("a[title='Ajouter un inscrit']")).click();
        takeFullPageScreenshot("ajouterParticipantASession.postClickAjouterUnInscrit");

        if (null != lieu) {
            // Sélection du lieu (devrait peut-être être préselectionné lorsqu'il n'y en a qu'un)
            // Code avant 1.1.5-SNAPSHOT
            // webDriver.findElement(By.xpath("//label[text()='Lieu']/..//span[@class='placeholder']")).click();
            webDriver.findElement(By.xpath("//label[text()='Lieu']/..//div[@class='ss-single']")).click();

            //webDriver.findElement(By.xpath("//label[text()='Lieu']/..//input[@type='search']")).click();
            // On sélectionne la première salle de la liste (mais pas l'entrée "---- Aucun ---")
            // Code avant 1.1.5-SNAPSHOT
            // webDriver.findElement(By.xpath("//label[text()='Lieu']/..//div[@class='ss-option']")).click();
            //webDriver.findElement(By.xpath("//div[contains(@class,'ss-option')][2]")).click();
            webDriver.findElement(By.xpath("//div[contains(@class,'ss-option')][text()='"+lieu+"']")).click();
        }

        boolean isModeLookup = true;

        if ("1.1.3".equals(getEsupEmargementVersion())) {
            // Je n'avais pas réussi a faire fonctionner le mode lookup en 1.1.3
            // a retester (après avoir été obligé d'utiliser le mode lookup du fait du bug en 1.1.4)
            isModeLookup = false;
        }

        if (!isModeLookup) {
            // FIXME Esup-Emargement 1.1.4 plante si l'on saisi l'adresse mail complète sans sélectionner dans la liste
            webDriver.findElement(By.xpath("//label[text()='Eppn']/..//input[@type='email']")).sendKeys(eppn);
        } else {
            // TODO extraire nom/prénom de l'eppn
            webDriver.findElement(By.xpath("//label[text()='Eppn']/..//input[@type='email']")).sendKeys(eppn.substring(0, 5));

            // Attendre l'affichage de la liste de choix des individus
            // TODO Ne pas supposer que le premier résultat est le bon mais vérifier dans la liste

            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(5));

            /*
            // Pas sûr que cela fonctionne à tous les coups (because id autocomplete ? à voir)
            String autoCompleteLineId = "awesomplete_list_2_item_0";
            wait.until(ExpectedConditions.presenceOfElementLocated(By.id(autoCompleteLineId)));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(autoCompleteLineId)));
            */
            String xpath = "//label[text()='Eppn']/..//li[1]";
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(xpath)));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)));


            // Sélectionner l'individu dans la liste
            // FIXME Ne fonctionne pas
            //webDriver.findElement(By.id(autoCompleteLineId)).click();
            webDriver.findElement(By.xpath(xpath)).click();
        }

        takeFullPageScreenshot("ajouterParticipantASession.saisieComplete");

        webDriver.findElement(By.cssSelector("input[value='Valider']")).click();
        takeFullPageScreenshot("ajouterParticipantASession.valide");
    }

    public void takeFullPageScreenshot() {
        takeFullPageScreenshot(null);
    }

    public void takeFullPageScreenshot(String title) {
        if (!isScreenshotEnabled) {
            return;
        }

        screenshotCounter++;
        File srcFile = ((FirefoxDriver)webDriver).getFullPageScreenshotAs(OutputType.FILE);
        try {
            FileUtils.copyFile(srcFile, new File("/tmp/screenshots."+screenshotCounter+(null!=title?"."+title:"")+".png"));
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    static public String getDateDDMMYYFromYYYYMMDD(String dateYYYYMMDD)
    {
        return getDateDDMMYYFromYYYYMMDD(dateYYYYMMDD, "-");
    }

    static public String getDateDDMMYYFromYYYYMMDD(String dateYYYYMMDD, String delimiter)
    {
        return dateYYYYMMDD.substring(8,10)+delimiter+dateYYYYMMDD.substring(5, 7)+delimiter+dateYYYYMMDD.substring(2, 4);
    }

    static public String getDateDDMMYYYYFromYYYYMMDD(String dateYYYYMMDD)
    {
        return getDateDDMMYYYYFromYYYYMMDD(dateYYYYMMDD, "-");
    }

    static public String getDateDDMMYYYYFromYYYYMMDD(String dateYYYYMMDD, String delimiter)
    {
        return dateYYYYMMDD.substring(8,10)+delimiter+dateYYYYMMDD.substring(5, 7)+delimiter+dateYYYYMMDD.substring(0, 4);
    }
}