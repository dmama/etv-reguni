# Paramètre : IN ou INPO en fonction de l'endroit où on veut effectivement déployer l'application
ENVIRONMENT="$1"
if [ "$ENVIRONMENT" != "IN" -a "$ENVIRONMENT" != "INPO" ]; then
	echo "!!! Le script doit pouvoir distinguer un déploiement en intégration d'un déploiement en intégration de post-production au travers d'un paramètre IN ou INPO !!!" >&2
	exit 1
fi

# On remonte sur le répertoire contenant unireg
cd ../..
if [ ! -d unireg ]; then
	echo "!!! Impossible de trouver le répertoire 'unireg' à partir du chemin $(pwd)" >&2
	exit 1
fi

DATE=$(date "+%Y-%m-%d_%H_%M_%S")


DEPLOY_ONLY=0
if [ "$2x" == "deployx" ]; then
	DEPLOY_ONLY=1
fi
echo "Deploy only: $DEPLOY_ONLY"


if [ $DEPLOY_ONLY == 0 ]; then
	svn update unireg
fi
if [ $? != 0 ]; then
	echo "!!! Erreur lors du svn update" >&2
	exit 1
fi

#########
# Version
version=$(grep "long=" unireg/base/version.txt|awk -F= '{ print $2; }')
#########
echo "Version: $version"

if [ "$ENVIRONMENT" == "INPO" ]; then
	env=integration-po
else
	env=integration
fi

user=dsi_unireg@ssv0309v
upDir=/ccv/data/dsi_unireg/uploads
baseDir=/ccv/data/dsi_unireg/unireg$ENVIRONMENT

webAppDir=$baseDir/applications/unireg-web
webDeployDir=$baseDir/app/unireg-web/${version}

wsAppDir=$baseDir/applications/unireg-ws
wsDeployDir=$baseDir/app/unireg-ws/${version}

webFileOrig=uniregweb-release.zip
webFileDest=uniregweb-release-${version}-${DATE}.zip
wsFileOrig=uniregws-release.zip
wsFileDest=uniregws-release-${version}-${DATE}.zip

# Compilation
if [ $DEPLOY_ONLY == 0 ]; then
	(cd unireg/base && mvn -Pnot,oracle,ext clean install)
fi
if [ $? != 0 ]; then
	echo "!!! Erreur lors du build" >&2
	exit 1
fi

if [ $DEPLOY_ONLY == 0 ]; then
	(cd unireg/web && mvn -Pnot,oracle assembly:assembly)
fi
if [ $? != 0 ]; then
	echo "!!! Erreur lors de l'assembly de web" >&2
	exit 1
fi

if [ $DEPLOY_ONLY == 0 ]; then
	(cd unireg/ws && mvn -Pnot,oracle assembly:assembly)
fi
if [ $? != 0 ]; then
	echo "!!! Erreur lors de l'assembly de ws" >&2
	exit 1
fi

cp -v unireg/web/target/$webFileOrig unireg/web/target/$webFileDest
cp -v unireg/ws/target/$wsFileOrig unireg/ws/target/$wsFileDest

#
# arrêt
#
ssh $user "cd $baseDir && ./tomcatctl.sh stop"
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
ssh $user "cd $baseDir && ./tomcatctl.sh clean && ./tomcatctl.sh start"
echo "Redémarrage de tomcat à $(date)"
