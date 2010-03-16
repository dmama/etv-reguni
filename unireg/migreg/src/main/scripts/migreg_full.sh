#!/bin/bash
# ce script permet de lancer la migration complète des débiteurs et des sourciers

DATE=`date +"%Y%m%d%H%M%S"`

# ces deux variables sont à modifier par l'exploitation avant l'exécution du script (la JVM doit être une version 1.5 !)
REPERTOIRE_LOGS=logs
JAVA_HOME=

# migration des débiteurs / listes récapitulatives
ant -Dlimit=DPI_FULL -DerrorsProcessing=false
mkdir -p $REPERTOIRE_LOGS/dpi_full_$DATE
mv *.log* $REPERTOIRE_LOGS/dpi_full_$DATE
mv *.csv* $REPERTOIRE_LOGS/dpi_full_$DATE

# migration des sourciers
ant -Dlimit=SOURCIER_FULL -DerrorsProcessing=false
mkdir -p $REPERTOIRE_LOGS/sourcier_full_$DATE
mv *.log* $REPERTOIRE_LOGS/sourcier_full_$DATE
mv *.csv* $REPERTOIRE_LOGS/sourcier_full_$DATE