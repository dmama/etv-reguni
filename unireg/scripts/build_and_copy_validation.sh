# On remonte sur le répertoire contenant unireg
cd ../..
if [ ! -d unireg ]; then
	echo "!!! Impossible de trouver le répertoire 'unireg' à partir du chemin $(pwd)"
	exit 1
fi

DATE=$(date "+%Y-%m-%d_%H_%M_%S")


# On fait l'update au début pour le numéro de version
svn update unireg
if [ $? != 0 ]; then
	echo "!!! Erreur lors du svn update"
	exit 1
fi

#########
# Version
env=Valid
release=0
version=$(grep "long=" unireg/base/version.txt|awk -F= '{ print $2; }')
version=$version.$release
#########

echo "Building version $version ($env)"

user=dsi_unireg@ssv0309v
relFileOrig=unireg-web-release.zip
relFileDest=unireg-web-release-${env}-${version}-${DATE}.zip
wsFileOrig=unireg-ws-release.zip	
wsFileDest=unireg-ws-release-${env}-${version}-${DATE}.zip
nexusFileOrig=unireg-nexus-release.zip	
nexusFileDest=unireg-nexus-release-${env}-${version}-${DATE}.zip
ubrFileOrig=ubr-release.zip
ubrFileDest=ubr-release-${env}-${version}-${DATE}.zip
MVN_OPTS="-Pnot,build.source,oracle,all,jspc"

# on vérifie que l'on ne dépend pas de librairies SNAPSHOT
IGNORE_SNAPSHOT="org.apache.activemq.protobuf:activemq-protobuf:jar:1.0-SNAPSHOT"
SNAPSHOT_PRESENT=$((cd unireg/base && mvn $MVN_OPTS dependency:list) | sed -e '/The following files have been resolved/,/^[INFO][[:blank:]]+$/ !D' | grep -v ":test$" | grep SNAPSHOT | grep -v $IGNORE_SNAPSHOT | grep -v "checking for updates from" | sort -u)
if [ -n "$SNAPSHOT_PRESENT" ]; then
	echo "$SNAPSHOT_PRESENT"
	echo "!!! Erreur : l'application dépend de librairies SNAPSHOT (voir liste ci-dessus). Veuillez modifier la configuration maven pour utiliser des versions fixes."
	exit 1
fi

(cd unireg/base && mvn $MVN_OPTS clean deploy)
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
echo "Les fichiers sont sur: $user:$REM_DIR/*-${env}-${version}-${DATE}.zip"

echo "Fin du déploiement at: $(date)"
