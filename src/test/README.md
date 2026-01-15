# Lancement des tests automatiques d'intégration Esup-Emargement

## Introduction
Les tests automatiques d'intégration consistent à simuler le comportement d'un (ou plusieurs) utilisateur(s) interagissant avec l'application via un navigateur.

Au lancement du test, un navigateur s'ouvre et vous pouvez observer le script dérouler les tests (clic par ci, saisie par là, etc).
Le résultat obtenu est comparé au resultat attendu et le test est mis en échec si une différence est constatée.

A ce stade, les tests automatiques ne couvrent qu'une partie des fonctionnalités d'Esup-Emargement et sont essentiellement orientés interfaçage avec ADE Campus (et plus particulièrement quelques nouvelles fonctionalités introduites après la 1.1.3) mais ils ont vocation à être enrichis.

##  Configuration de l'environnement de test

1 - Installation de gecko

2 - Configuration d'un projet ADE de test

3 - Configuration du script de test

4 - Adaptation du driver CAS (si besoin)

### 1 - Installation de gecko

Récuperer une archive sur https://github.com/mozilla/geckodriver/releases

#### Sous linux

Installation réalisée sous Debian 12/Bookworm avec le package _geckodriver-v0.36.0-linux64.tar.gz_

A supposer le fichier téléchargée sous _/tmp_, lancer les commandes
```
# mkdir /usr/local/geckodriver-0.36.0`
# cd /usr/local/geckodriver-0.36.0`
# tar zxvf /tmp/geckodriver-v0.36.0-linux64.tar.gz
```

Si vous préferez l'installer à un autre emplacement, pensez à adapter le chemin indiqué dans le fichier [src/test/java/SeleniumConfig.java](java/SeleniumConfig.java)

### 2 - Configuration d'un projet ADE de test

Les tests liés à ADE sont basés sur une configuration (ressources, activités, placement dans l'emploi du temps) donnée de projet ADE.

Il est donc nécessaire de créer un projet ADE de test au contenu fidèle à ce qui est décrit dans [src/test/resources/ade_content](resources/ade_content).

L'identifiant de ce projet (i.e. numéro de projet) doit être noté et devra être précisé dans la configuration du script de test (voir paragraphe suivant).

### 3 - Configuration du script de test

Les paramètres du script de test sont déclarés dans le fichier _src/test/resources/testng-parameters.xml_. Si ce dernier n'existe pas faire une copie du fichier [src/test/resources/testng-parameters.dist.xml](resources/testng-parameters.dist.xml) sous _src/test/resources/testng-parameters.xml_

### 4 - Adaptation du driver CAS (si besoin)

Le script interagisant avec l'interface d'authentification CAS part du principe que les champs de saisie de l'identifiant et du mot de passe portent respectivement un id (champ "id" de la balise HTML) "username" et "password" et que le bouton de validation est la balise HTML (supposée unique dans la page) ayant pour attribut "type" la valeur "submit".

Si tel n'est pas le cas pour votre page d'authentification CAS (ou si l'authentification passe par plusieurs étapes) vous pourriez être amené à modifier le fichier [src/test/java/CasDriver.java](java/CasDriver.java)

## Lancement des tests
`mvn verify`

Eventuellement `mvn clean verify`