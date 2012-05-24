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

nexusAppDir=$baseDir/applications/unireg-nexus
nexusDeployDir=$baseDir/app/unireg-nexus/${version}

webFileOrig=uniregweb-release.zip
wsFileOrig=uniregws-release.zip
nexusFileOrig=uniregnexus-release.zip

#########

function compile_all() {

  if [ $DEPLOY_ONLY == 0 ]; then
	  (cd unireg/base && mvn -Pnot,oracle,ext clean install)
  fi
  if [ $? != 0 ]; then
	  echo "!!! Erreur lors du build" >&2
	  exit 1
  fi

}

function assemble_app() {
  appName = $1

  if [ $DEPLOY_ONLY == 0 ]; then
	  (cd unireg/$appName && mvn -Pnot,oracle assembly:assembly)
  fi
  if [ $? != 0 ]; then
	  echo "!!! Erreur lors de l'assembly de $appName" >&2
	  exit 1
  fi

  cp -v unireg/web/target/$webFileOrig unireg/web/target/$webFileDest
}

function copy_with_timestamp() {
  inputFile = $1
  dirFile = $(dirname $inputFile)
  outputFile = $(basename $inputFile .zip)-${version}-${DATE}.zip

  cp -v $inputFile $dirFile/$outputFile
  echo "$dirFile/$outputFile"
}

function deploy_app() {
  zipFile = $1
  appDir = $2
  deployDir = $3

  scp unireg/*/target/$zipFile $user:$upDir/

  ssh $user "rm -rf $upDir/explode && mkdir -p $upDir/explode"
  ssh $user "cd $upDir/explode && unzip $upDir/$zipFile"

  # copie des fichiers de config
  ssh $user "mkdir -p $appDir/config"
  ssh $user "cp $upDir/explode/config/$env/* $appDir/config"

  # copie du war
  ssh $user "mkdir -p $deployDir/deployment"
  ssh $user "cp $upDir/explode/deployment/*.war $deployDir/deployment/"

  # mise-à-jour du lien symbolique
  ssh $user "cd $appDir && rm -f deployment && ln -s $deployDir/deployment deployment"
}

#########

# Compilation
compile_all
assemble_app web
assemble_app ws
assemble_app nexus

# add timestamps to zip files
webFileDest=$(copy_with_timestamp unireg/web/target/$webFileOrig)
wsFileDest=$(copy_with_timestamp unireg/ws/target/$wsFileOrig)
nexusFileDest=$(copy_with_timestamp unireg/nexus/target/$nexusFileOrig)

# arrêt
ssh $user "cd $baseDir && ./tomcatctl.sh stop"
echo "Arrêt de tomcat à $(date)"

# Deploiement de nexus
deploy_app $nexusFileDest $nexusAppDir $nexusDeployDir
echo "Fin du deploiement de nexus à: $(date)"

# Deploiement de la web-app
deploy_app $webFileDest $webAppDir $webDeployDir
echo "Fin du deploiement de la web-app à: $(date)"

# Deploiement des web-services
deploy_app $wsFileDest $wsAppDir $wsDeployDir
echo "Fin du deploiement des web-services à: $(date)"

# cleanup & restart
ssh $user "cd $baseDir && ./tomcatctl.sh clean && ./tomcatctl.sh start"
echo "Redémarrage de tomcat à $(date)"
