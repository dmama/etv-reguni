#!/usr/bin/env bash

# On vérifie que le script s'exécute depuis le bon répertoire
if [ ! -f base/version.txt ]; then
	echo "!!! Le script doit être lancé depuis le répertoire racine du projet (chemin actuel = $(pwd)) !!!" >&2
	exit 1
fi

DATE=$(date "+%Y-%m-%d_%H_%M_%S")

DEPOSANT="$1"
if [[ ! "$DEPOSANT" =~ ^[a-zA-Z0-9]{6}$ ]]; then
	echo "!!! L'identifiant du déposant ($DEPOSANT) n'est pas un hexagramme valide."
	exit 1
fi


#########
# Version
version=$(grep "long=" base/version.txt|awk -F= '{ print $2; }')
#########

echo "Building version $version"

relFileOrig=unireg-web-release.zip
relFileDest=unireg-web-release-${version}-${DATE}.zip
wsFileOrig=unireg-ws-release.zip	
wsFileDest=unireg-ws-release-${version}-${DATE}.zip
nexusFileOrig=unireg-nexus-release.zip	
nexusFileDest=unireg-nexus-release-${version}-${DATE}.zip
ubrFileOrig=ubr-release.zip
ubrFileDest=ubr-release-${version}-${DATE}.zip
MVN_OPTS="-Pnot,build.source,oracle,all,jspc"

# on vérifie que l'on ne dépend pas de librairies SNAPSHOT
IGNORE_SNAPSHOT="org.apache.activemq.protobuf:activemq-protobuf:jar:1.0-SNAPSHOT"
SNAPSHOT_PRESENT=$((cd base && mvn $MVN_OPTS dependency:list) | sed -e '/The following files have been resolved/,/^[INFO][[:blank:]]+$/ !D' | grep -v ":test$" | grep SNAPSHOT | grep -v $IGNORE_SNAPSHOT | grep -v "checking for updates from" | sort -u)
if [ -n "$SNAPSHOT_PRESENT" ]; then
	echo "$SNAPSHOT_PRESENT"
	echo "!!! Erreur : l'application dépend de librairies SNAPSHOT (voir liste ci-dessus). Veuillez modifier la configuration maven pour utiliser des versions fixes."
	exit 1
fi

(cd base && mvn $MVN_OPTS clean install)
if [ $? != 0 ]; then
	echo "!!! Erreur lors du build"
	exit 1
fi

(cd nexus && mvn $MVN_OPTS assembly:assembly)
if [ $? != 0 ]; then
	echo "!!! Erreur lors de l'assembly de nexus"
	exit 1
fi

(cd web && mvn $MVN_OPTS assembly:assembly)
if [ $? != 0 ]; then
	echo "!!! Erreur lors de l'assembly de web"
	exit 1
fi

(cd ws && mvn $MVN_OPTS assembly:assembly)
if [ $? != 0 ]; then
	echo "!!! Erreur lors de l'assembly de ws"
	exit 1
fi

(cd ubr && mvn $MVN_OPTS assembly:assembly)
if [ $? != 0 ]; then
	echo "!!! Erreur lors de l'assembly de ubr"
	exit 1
fi

# Renommage des fichiers ZIP avec la date
cp nexus/target/$nexusFileOrig nexus/target/$nexusFileDest
cp web/target/$relFileOrig web/target/$relFileDest
cp ws/target/$wsFileOrig ws/target/$wsFileDest
cp ubr/target/$ubrFileOrig ubr/target/$ubrFileDest

# dépôt direct dans le système du CEI
URL_DEPOT=http://exploitation.etat-de-vaud.ch/outils/web/ws/rest/file/upload
CEI_GROUPE_CIBLE=WEB
curl -X POST --form from=$DEPOSANT --form to=$CEI_GROUPE_CIBLE --form file=@$(pwd)/nexus/target/$nexusFileDest $URL_DEPOT
echo
curl -X POST --form from=$DEPOSANT --form to=$CEI_GROUPE_CIBLE --form file=@$(pwd)/web/target/$relFileDest $URL_DEPOT
echo
curl -X POST --form from=$DEPOSANT --form to=$CEI_GROUPE_CIBLE --form file=@$(pwd)/ws/target/$wsFileDest $URL_DEPOT
echo
curl -X POST --form from=$DEPOSANT --form to=$CEI_GROUPE_CIBLE --form file=@$(pwd)/ubr/target/$ubrFileDest $URL_DEPOT
echo


echo "Fin du déploiement at: $(date)"
