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

webAppDir=/ccv/data/dsi_unireg/uniregIN/applications/unireg-web
webDeployDir=/ccv/data/dsi_unireg/uniregIN/app/unireg-web/${version}

wsAppDir=/ccv/data/dsi_unireg/uniregIN/applications/unireg-ws
wsDeployDir=/ccv/data/dsi_unireg/uniregIN/app/unireg-ws/${version}

webFileOrig=uniregweb-release.zip
webFileDest=uniregweb-release-${version}-${DATE}.zip
wsFileOrig=uniregws-release.zip
wsFileDest=uniregws-release-${version}-${DATE}.zip

# Compilation
if [ $DEPLOY_ONLY == 0 ]; then
	(cd unireg/base && mvn -Pnot,oracle,ext clean install)
fi
if [ $? != 0 ]; then
	echo "!!! Erreur lors du build"
	exit 1
fi

if [ $DEPLOY_ONLY == 0 ]; then
	(cd unireg/web && mvn -Pnot,oracle assembly:assembly)
fi
if [ $? != 0 ]; then
	echo "!!! Erreur lors de l'assembly de web"
	exit 1
fi

if [ $DEPLOY_ONLY == 0 ]; then
	(cd unireg/ws && mvn -Pnot,oracle assembly:assembly)
fi
if [ $? != 0 ]; then
	echo "!!! Erreur lors de l'assembly de ws"
	exit 1
fi

cp -v unireg/web/target/$webFileOrig unireg/web/target/$webFileDest
cp -v unireg/ws/target/$wsFileOrig unireg/ws/target/$wsFileDest

#
# arrêt
#
ssh $user "cd /ccv/data/dsi_unireg/uniregIN && ./tomcatctl.sh stop"
echo "Arrêt de tomcat à $(date)"

#
# Deploiement de la web-app
#
scp unireg/web/target/$webFileDest $user:$upDir/

ssh $user "rm -rf $upDir/explode && mkdir -p $upDir/explode"
ssh $user "cd $upDir/explode && unzip $upDir/$webFileDest"

# copie des fichiers de config
ssh $user "mkdir -p $webAppDir/config"
ssh $user "cp $upDir/explode/config/$env/* $webAppDir/config"

# copie du war
ssh $user "mkdir -p $webDeployDir/deployment"
ssh $user "cp $upDir/explode/deployment/uniregweb.war $webDeployDir/deployment/unireg-web.war"

# mise-à-jour du lien symbolique
ssh $user "cd $webAppDir && rm -f deployment && ln -s $webDeployDir/deployment deployment"
echo "Fin du deploiement de la web-app à: $(date)"

#
# Deploiement des web-services
#
scp unireg/ws/target/$wsFileDest $user:$upDir/

ssh $user "rm -rf $upDir/explode && mkdir -p $upDir/explode"
ssh $user "cd $upDir/explode && unzip $upDir/$wsFileDest"

# copie des fichiers de config
ssh $user "mkdir -p $wsAppDir/config"
ssh $user "cp $upDir/explode/config/$env/* $wsAppDir/config"

# copie du war
ssh $user "mkdir -p $wsDeployDir/deployment"
ssh $user "cp $upDir/explode/deployment/uniregws.war $wsDeployDir/deployment/unireg-ws.war"

# mise-à-jour du lien symbolique
ssh $user "cd $wsAppDir && rm -f deployment && ln -s $wsDeployDir/deployment deployment"
echo "Fin du deploiement des web-services à: $(date)"

#
# cleanup & restart
#
ssh $user "cd /ccv/data/dsi_unireg/uniregIN && ./tomcatctl.sh clean && ./tomcatctl.sh start"
echo "Redémarrage de tomcat à $(date)"
