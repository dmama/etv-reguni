# On remonte sur le répertoire contenant unireg
cd ../..
if [ ! -d unireg ]; then
	echo "!!! Impossible de trouver le répertoire 'unireg' à partir du chemin $(pwd)"
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
version=$(grep "long=" unireg/base/version.txt|awk -F= '{ print $2; }')
#########

echo "Building version $version"

user=dsi_unireg@ssv0309v
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
SNAPSHOT_PRESENT=$((cd unireg/base && mvn $MVN_OPTS dependency:list) | sed -e '/The following files have been resolved/,/^[INFO][[:blank:]]+$/ !D' | grep -v ":test$" | grep SNAPSHOT | grep -v $IGNORE_SNAPSHOT | grep -v "checking for updates from" | sort -u)
if [ -n "$SNAPSHOT_PRESENT" ]; then
	echo "$SNAPSHOT_PRESENT"
	echo "!!! Erreur : l'application dépend de librairies SNAPSHOT (voir liste ci-dessus). Veuillez modifier la configuration maven pour utiliser des versions fixes."
	exit 1
fi

# JDE 27.06.2013 : on ne publie plus dans NEXUS (pour gagner de la place...), seulement dans le m2 local
#(cd unireg/base && mvn $MVN_OPTS clean deploy)
(cd unireg/base && mvn $MVN_OPTS clean install)
if [ $? != 0 ]; then
	echo "!!! Erreur lors du build"
	exit 1
fi

(cd unireg/nexus && mvn $MVN_OPTS assembly:assembly)
if [ $? != 0 ]; then
	echo "!!! Erreur lors de l'assembly de nexus"
	exit 1
fi

(cd unireg/web && mvn $MVN_OPTS assembly:assembly)
if [ $? != 0 ]; then
	echo "!!! Erreur lors de l'assembly de web"
	exit 1
fi

(cd unireg/ws && mvn $MVN_OPTS assembly:assembly)
if [ $? != 0 ]; then
	echo "!!! Erreur lors de l'assembly de ws"
	exit 1
fi

(cd unireg/ubr && mvn $MVN_OPTS assembly:assembly)
if [ $? != 0 ]; then
	echo "!!! Erreur lors de l'assembly de ubr"
	exit 1
fi

# Renommage des fichiers ZIP avec la date
cp unireg/nexus/target/$nexusFileOrig unireg/nexus/target/$nexusFileDest
cp unireg/web/target/$relFileOrig unireg/web/target/$relFileDest
cp unireg/ws/target/$wsFileOrig unireg/ws/target/$wsFileDest
cp unireg/ubr/target/$ubrFileOrig unireg/ubr/target/$ubrFileDest

# Deploiement sur ssv0309v
REM_DIR="~/release"
ssh $user "mkdir $REM_DIR"
scp unireg/nexus/target/$nexusFileDest $user:$REM_DIR
scp unireg/web/target/$relFileDest $user:$REM_DIR
scp unireg/ws/target/$wsFileDest $user:$REM_DIR
scp unireg/ubr/target/$ubrFileDest $user:$REM_DIR
echo "Les fichiers sont sur: $user:$REM_DIR/*-${version}-${DATE}.zip"

# dépôt direct dans le système du CEI
URL_DEPOT=http://exploitation.etat-de-vaud.ch/outils/web/ws/rest/file/upload
CEI_GROUPE_CIBLE=WEB
curl -X POST --form from=$DEPOSANT --form to=$CEI_GROUPE_CIBLE --form file=@$(pwd)/unireg/nexus/target/$nexusFileDest $URL_DEPOT
echo
curl -X POST --form from=$DEPOSANT --form to=$CEI_GROUPE_CIBLE --form file=@$(pwd)/unireg/web/target/$relFileDest $URL_DEPOT
echo
curl -X POST --form from=$DEPOSANT --form to=$CEI_GROUPE_CIBLE --form file=@$(pwd)/unireg/ws/target/$wsFileDest $URL_DEPOT
echo
curl -X POST --form from=$DEPOSANT --form to=$CEI_GROUPE_CIBLE --form file=@$(pwd)/unireg/ubr/target/$ubrFileDest $URL_DEPOT
echo


echo "Fin du déploiement at: $(date)"
