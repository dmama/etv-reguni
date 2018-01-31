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
release=0
version=$(grep "long=" unireg/base/version.txt|awk -F= '{ print $2; }')
version=$version.$release
#########

echo "Building version $version (standalone)"

relFileOrig=uniregctb-release.zip
relFileDest=unireg-standalone-${version}-${DATE}.zip
wsFileOrig=uniregws-release.zip
wsFileDest=uniregws-standalone-${version}-${DATE}.zip
MVN_OPTS="-Pnot,oracle,env.standalone"


(cd unireg/base && mvn $MVN_OPTS clean install)
if [ $? != 0 ]; then
	echo "!!! Erreur lors du build"
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


# Renommage des fichiers ZIP avec la date
cp unireg/web/target/$relFileOrig unireg/$relFileDest
cp unireg/ws/target/$wsFileOrig unireg/$wsFileDest

echo "Les fichiers sont disponibles ici:"
echo "  => $(pwd)/unireg/$relFileDest"
echo "  => $(pwd)/unireg/$wsFileDest"
echo "Fin du deploiement at: $(date)"
