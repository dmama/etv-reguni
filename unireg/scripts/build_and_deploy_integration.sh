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
version=$(grep "long=" unireg/base/version.txt|awk -F= '{ print $2; }')
#########
echo "Version: $version"

env=integration
user=dsi_unireg@ssv0309v
upDir=/ccv/data/dsi_unireg/uploads

configDir=/ccv/data/dsi_unireg/cat_uniregI/app/unireg/config
deployDir=/ccv/data/dsi_unireg/cat_uniregI/webapps/fiscalite#int-unireg#web
workDir=/ccv/data/dsi_unireg/cat_uniregI/work/unireg

wsConfigDir=/ccv/data/dsi_unireg/cat_uniregI/app/unireg-ws/config
wsDeployDir=/ccv/data/dsi_unireg/cat_uniregI/webapps/fiscalite#int-unireg#ws
wsWorkDir=/ccv/data/dsi_unireg/cat_uniregI/work/unireg-ws

relFileOrig=uniregweb-release.zip
relFileDest=uniregweb-release-${version}-${DATE}.zip
wsFileOrig=uniregws-release.zip
wsFileDest=uniregws-release-${version}-${DATE}.zip

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
	echo "!!! Erreur lors de l'assembly de web"
	exit 1
fi

if [ $DEPLOY_ONLY == 0 ]; then
	(cd unireg/ws && mvn -Pnot,env.int,oracle assembly:assembly)
fi
if [ $? != 0 ]; then
	echo "!!! Erreur lors de l'assembly de ws"
	exit 1
fi

cp -v unireg/web/target/$relFileOrig unireg/web/target/$relFileDest
cp -v unireg/ws/target/$wsFileOrig unireg/ws/target/$wsFileDest

#
# Deploiement de la web-app
#
scp unireg/web/target/$relFileDest $user:$upDir/

ssh $user "rm -rf $upDir/explode"
ssh $user "mkdir -p $upDir/explode"
ssh $user "cd $upDir/explode && unzip $upDir/$relFileDest"

# copie des fichiers de config
ssh $user "mkdir -p $configDir/"
ssh $user "cp $upDir/explode/config/$env/* $configDir/"

# unzip du war
ssh $user "rm -rf $deployDir $workDir"
ssh $user "mkdir -p $deployDir"
ssh $user "cd $deployDir && unzip $upDir/explode/deployment/uniregweb.war"

echo "Fin du deploiement de la web-app à: $(date)"

#
# Deploiement des web-services
#
scp unireg/ws/target/$wsFileDest $user:$upDir/

ssh $user "rm -rf $upDir/explode"
ssh $user "mkdir -p $upDir/explode"
ssh $user "cd $upDir/explode && unzip $upDir/$wsFileDest"

# copie des fichiers de config
ssh $user "mkdir -p $wsConfigDir/"
ssh $user "cp $upDir/explode/config/$env/* $wsConfigDir/"

# unzip du war
ssh $user "rm -rf $wsDeployDir $wsWorkDir"
ssh $user "mkdir -p $wsDeployDir"
ssh $user "cd $wsDeployDir && unzip $upDir/explode/deployment/uniregws.war"

echo "Fin du deploiement des web-services à: $(date)"

