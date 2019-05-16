# Unireg

Unireg est le registre des contribuables du Canton de Vaud.

## Liens

 * [Page Wiki du projet Unireg](https://portail.etat-de-vaud.ch/outils/dsiwiki/display/FiscaliteUNIREG/Unireg).
   * [Marche-à-suivre pour déployer Unireg sur un poste de développement](https://portail.etat-de-vaud.ch/outils/dsiwiki/pages/viewpage.action?pageId=517472266).
   * [Vue d'ensemble de l'architecture Unireg](https://portail.etat-de-vaud.ch/outils/dsiwiki/display/FiscaliteUNIREG/Vue+d%27ensemble+de+l%27architecture+d%27Unireg).
   * [Environnements, utilisateurs et mots-de-passe Unireg](https://portail.etat-de-vaud.ch/outils/dsiwiki/display/FiscaliteUNIREG/Environnements%2C+utilisateurs+et+mots+de+passes#Environnements,utilisateursetmotsdepasses-Fidor) (page protégée).
 * [Intégration continue Unireg](http://slv3479d.etat-de-vaud.ch:55080/outils/jenkins-unireg/view/Master/) (Jenkins).
 * [Ecran de monitoring Unireg et Fidor](http://slv2984v.etat-de-vaud.ch:8050/#/).
 * [Bug tracking system](https://portail.etat-de-vaud.ch/outils/jira/issues/?jql=project%20%3D%20SIFISC%20AND%20component%20%3D%20Unireg) (JIRA).
 

## Démarrage rapide

 1. compilation : `$ (cd base && mvn -Pnot,dev,all clean install)`
 2. exécution des tests unitaires : `$ (cd base && mvn test)`
 3. configuration de la DB des tests d'intégration : `$ cp base/unireg-ut.properties.sample base/unireg-ut.properties` + renseignement de l'utilisateur/mot-de-passe Orale
 4. exécution des tests d'intération : `$ (cd business-it && mvn test)` 
