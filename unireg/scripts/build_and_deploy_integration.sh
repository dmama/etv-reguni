# On remonte sur le répertoire contenant unireg
cd ../..
if [ ! -d unireg ]; then
	echo "!!! Impossible de trouver le répertoire 'unireg' à partir du chemin $(pwd)"
	exit 1
fi

DATE=$(date "+%Y-%m-%d_%H_%M_%S")


DEPLOY_ONLY=0
if [ "$1x" == "deployx" ]; then
	DEPLOY_ONLY=1
fi
echo "Deploy only: $DEPLOY_ONLY"


if [ $DEPLOY_ONLY == 0 ]; then
	svn update unireg
fi
if [ $? != 0 ]; then
	echo "!!! Erreur lors du svn update"
	exit 1
fi

#########
# Version
lversion=$(grep "long=" unireg/base/version.txt|awk -F= '{ print $2; }')
sversion=$(grep "short=" unireg/base/version.txt|awk -F= '{ print $2; }')
#########
echo "Version: $sversion / $lversion"

user=dsi_unireg@ssv0309v
upDir=/ccv/data/dsi_unireg/uploads
configDir=/ccv/data/dsi_unireg/cat_uniregI/app/unireg-is/config
deployDir=/ccv/data/dsi_unireg/cat_uniregI/webapps/fiscalite#int-unireg-is
workDir=/ccv/data/dsi_unireg/cat_uniregI/work/unireg-is
env=integration

relFileOrig=uniregctb-release.zip
relFileDest=uniregctb-release-${sversion}-SNAP-${DATE}.zip

# Compilation
if [ $DEPLOY_ONLY == 0 ]; then
	(cd unireg/base && mvn -Pnot,env.int,oracle,ext clean install)
fi
if [ $? != 0 ]; then
	echo "!!! Erreur lors du build"
	exit 1
fi

if [ $DEPLOY_ONLY == 0 ]; then
	(cd unireg/web && mvn -Pnot,env.int,oracle assembly:assembly)
fi
if [ $? != 0 ]; then
	echo "!!! Erreur lors du build"
	exit 1
fi

# Deploiement
cp -v unireg/web/target/$relFileOrig unireg/web/target/$relFileDest
scp unireg/web/target/$relFileDest $user:$upDir/
ssh $user "rm -rf $upDir/explode"
ssh $user "mkdir -p $upDir/explode"
ssh $user "cd $upDir/explode && unzip $upDir/$relFileDest"

# copie des fichiers de config
ssh $user "mkdir -p $configDir/"
ssh $user "cp $upDir/explode/config/$env/* $configDir/"

# ajoute le suffixe '-pm' au fichier de log
ssh $user "sed 's/unireg-web\.log/unireg-web-pm.log/' $upDir/explode/config/$env/unireg-log4j.xml > $configDir/unireg-log4j.xml"

# unzip du war
ssh $user "rm -rf $deployDir"
ssh $user "rm -rf $workDir"
ssh $user "mkdir -p $deployDir"
ssh $user "cd $deployDir && unzip $upDir/explode/deployment/uniregctb.war"


echo "Fin du deploiement at: $(date)"

