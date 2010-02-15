#!/bin/bash
# ce script permet de lancer le rattrapage de la migration

DATE=`date +"%Y%m%d%H%M%S"`

# ces deux variables sont à modifier par l'exploitation avant l'exécution du script (la JVM doit être une version 1.5 !)
REPERTOIRE_LOGS=logs
JAVA_HOME=/usr/lib/jvm/java-1.5.0-sun

# Créations des fors sources
ant -Dobjet=DOUBLON 
mkdir -p $REPERTOIRE_LOGS/rattrapage_$DATE
mkdir -p $REPERTOIRE_LOGS/Rapports_$DATE
mv *.log* $REPERTOIRE_LOGS/rattrapage_$DATE
mv *.csv  $REPERTOIRE_LOGS/Rapports_$DATE
