# On remonte sur le répertoire contenant unireg
cd ../..
if [ ! -d unireg ]; then
	echo "!!! Impossible de trouver le répertoire 'unireg' à partir du chemin $(pwd)" >&2
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
	echo "!!! Erreur lors du svn update" >&2
	exit 1
fi

#########
# Version
version=$(grep "long=" unireg/base/version.txt|awk -F= '{ print $2; }')
#########
echo "Version: $version"

env=integration-te
user=unireg@spip
upDir=/ccv/home/unireg/uploads
tomcatDir=/ccv/home/unireg/apache-tomcat

webFileOrig=uniregweb-release.zip
webFileDest=uniregweb-release-${version}-${DATE}.zip
wsFileOrig=uniregws-release.zip
wsFileDest=uniregws-release-${version}-${DATE}.zip

webAppDir=$tomcatDir/appDir/unireg-web
wsAppDir=$tomcatDir/appDir/unireg-ws

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
ssh $user "cd $tomcatDir && bin/catalina.sh stop"
echo "Arrêt de tomcat à $(date)"

#
# Deploiement de la web-app
#

ssh $user "rm -rf $upDir && mkdir -p $upDir/explode"
scp unireg/web/target/$webFileDest $user:$upDir/
ssh $user "cd $upDir/explode && unzip $upDir/$webFileDest"


# copie des fichiers de config
ssh $user "mkdir -p $webAppDir/config"
ssh $user "cp $upDir/explode/config/$env/* $webAppDir/config"

# copie du war
ssh $user "mkdir -p $tomcatDir/webapps"
ssh $user "rm -rf $tomcatDir/webapps/fiscalite#unireg#web*"
ssh $user "cp $upDir/explode/deployment/uniregweb.war $tomcatDir/webapps/fiscalite#unireg#web.war"

echo "Fin du deploiement de la web-app à: $(date)"

#
# Deploiement des web-services
#

ssh $user "rm -rf $upDir/explode && mkdir -p $upDir/explode"
scp unireg/ws/target/$wsFileDest $user:$upDir/
ssh $user "cd $upDir/explode && unzip $upDir/$wsFileDest"

# copie des fichiers de config
ssh $user "mkdir -p $wsAppDir/config"
ssh $user "cp $upDir/explode/config/$env/* $wsAppDir/config"

# copie du war
ssh $user "mkdir -p $tomcatDir/webapps"
ssh $user "rm -rf $tomcatDir/webapps/fiscalite#unireg#ws*"
ssh $user "cp $upDir/explode/deployment/uniregws.war $tomcatDir/webapps/fiscalite#unireg#ws.war"

echo "Fin du deploiement des web-services à: $(date)"

#
# cleanup & restart
#
ssh $user "cd $tomcatDir && rm -rf work/*"
ssh $user "cd $tomcatDir && bin/catalina.sh start"
echo "Redémarrage de tomcat à $(date)"
