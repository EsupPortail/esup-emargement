# Esup-emargement
Esup-emargement est une application multi-contexte permettant l'émargement et le contrôle de présence en lien avec le système d'information.

============================

https://www.esup-portail.org/wiki/display/EMARGEMENT

## Éléments requis

### Pré-requis

* Java Open JDK 8 : https://openjdk.java.net/install : le mieux est de l'installer via le système de paquets de votre linux.

* Maven (dernière version 3.0.x) : http://maven.apache.org/download.cgi : le mieux est de l'installer via le système de paquets de votre linux.
* Postgresql 9 : le mieux est de l'installer via le système de paquets de votre linux.
* Tomcat 8/9
* un serveur CAS pour l'authentification / identification
* un annuaire Ldap permettant :
  * de donner les droits "Super-admin"
  * d'effectuer des recherches de groupes pour la constitution de listes d'émargement
  * d'effectuer des recherches d'un simple utilisateur pour insertion de celui-ci
    Esup-nfc-tag serveur et client afin de valider les badgeages effectués. Page dédiée : Esup-nfc-tag

## PostgreSQL

pg_hba.conf : ajout de

    host    all             all             127.0.0.1/32            password

redémarrage de postgresql

    psql
    create database esupemargement;
    create USER esupemargement with password 'esup';
    grant ALL ON DATABASE esupemargement to esupemargement;
    ALTER DATABASE esupemargement OWNER TO esupemargement;

### JDBC

Copier le jar JDBC dans le répertoire /src/main/resources puis déployer le dans le .m2

    mvn install:install-file  -Dfile=/opt/esup-emargement/src/main/resources/lib/ojdbc7.jar -DgroupId=com.oracle -DartifactId=ojdbc7 -Dversion=4.0 -Dpackaging=jar

### Paramétrage mémoire JVM :

Pensez à paramétrer les espaces mémoire JVM :

    export JAVA_OPTS="-Xms256m -Xmx256m"

### Création des tables :

modification de la configuration spring.jpa.hibernate.ddl-auto dans esup-emargement.properties :

passage de update à create

démarrage de l'application esup-emargement (via spring-boot, tomcat, ou autre, cf ci-dessous) pour création effective des tables

puis on repositionne spring.jpa.hibernate.ddl-auto à update pour que les prochains redémarrages n'écrasent pas les tables et donc les données

### Lancement d'esup-emargement via spring-boot :

    mvn spring-boot:run

### Obtention du war pour déploiement sur tomcat ou autre :

    mvn clean package

### esup-nfc-tag-server config

-urls ws

    <bean id="emargementJavaExtApi" class="org.esupportail.nfctag.service.api.impl.AppliExtRestWs">
        <property name="isTagableUrl" value="https://esup-emargement.univ-ville.fr/wsrest/nfc/isTagable"/>
        <property name="validateTagUrl" value="https://esup-emargement.univ-ville.fr/wsrest/nfc/validateTag"/>
        <property name="getLocationsUrl" value="https://esup-emargement.univ-ville.fr/wsrest/nfc/locations"/>
        <property name="description" value="Web Service Emargement-java test"/>
        <!--property name="backgroundColor" value="rgb(121, 119, 0)"/-->
        <property name="header" value="https://esup-emargement.univ-ville.fr/resources/images/logo.png"/>      
    </bean>

-tagIdcheck

    <bean id="tagIdCheckApiEsupSgc" class="org.esupportail.nfctag.service.api.impl.TagIdCheckRestWs">
        <property name="tagIdCheckUrl" value="https://esup-sgc.univ-ville.fr/wsrest/nfc/tagIdCheck"/>
        <property name="idFromEppnInitUrl" value="https://esup-sgc.univ-ville.fr/wsrest/nfc/idFromEppnInit"/>
        <property name="description" value="via Esup SGC"/>
    </bean>

### Téléchargement

Vous pourrez trouver la dernière version d'esup-emargementr sur Github :

https://github.com/EsupPortail/esup-emargement

### Contacts

Merci de vous abonner et d'utiliser la liste mail "esup-utilisateurs" pour nous contacter (problèmes techniques notamment) :

https://listes.esup-portail.org/sympa/info/esup-utilisateurs


### Crédits & Licence

Copyright (C) 2020 Esup Portail http://www.esup-portail.org
@Author (C) 2020 Jean-Pierre Tran <Jean-Pierre.Tran@univ-rouen.fr>
@Contributor (C) 2020 Vincent Bonamy <Vincent.Bonamy@univ-rouen.fr>
@Contributor (C) 2025 Damien Heute - Le Mans Université 
@Contributor (C) 2024 Delphine Boulanger - Université de Bretagne Occidentale
@Contributor (C) 2022 Vincent Rivière - Université Paris 1 Panthéon-Sorbonne
@Contributor (C) 2020 Fabrice Sebbe- Université de Rouen Normandie
@Contributor (C) 2020 Dominique Wormser- Université de Rouen Normandie
@Contributor (C) 2020 Olivier Lefebvre- Université de Rouen Normandie
@Contributor (C) 2020 Hugo Sadaune- Université de Rouen Normandie
@Contributor (C) 2020 Gauthier Girot- Université de Rouen Normandie

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

