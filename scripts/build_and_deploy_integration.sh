#!/usr/bin/env bash

# Paramètre : IN ou INPO en fonction de l'endroit où on veut effectivement déployer l'application
ENVIRONMENT="$1"
if [ "$ENVIRONMENT" != "IN" -a "$ENVIRONMENT" != "INPO" ]; then
	echo "!!! Le script doit pouvoir distinguer un déploiement en intégration d'un déploiement en intégration de post-production au travers d'un paramètre IN ou INPO !!!" >&2
	exit 1
fi

# On vérifie que le script s'exécute depuis le bon répertoire
if [ ! -f base/version.txt ]; then
	echo "!!! Le script doit être lancé depuis le répertoire racine du projet (chemin actuel = $(pwd)) !!!" >&2
	exit 1
fi

DATE=$(date "+%Y-%m-%d_%H_%M_%S")


DEPLOY_ONLY=0
if [ "$2x" == "deployx" ]; then
	DEPLOY_ONLY=1
fi
echo "Deploy only: $DEPLOY_ONLY"


#########
# Version
version=$(grep "long=" base/version.txt|awk -F= '{ print $2; }')
#########
echo "Version: $version"

if [ "$ENVIRONMENT" == "INPO" ]; then
	env=integration-po
	user=dsi_unireg@slv2655v
else
	env=integration
	user=dsi_unireg@slv2655v
fi

upDir=/ccv/data/dsi_unireg/uploads
baseDir=/ccv/data/dsi_unireg/unireg$ENVIRONMENT

webAppDir=$baseDir/applications/unireg-web
webDeployDir=$baseDir/app/unireg-web/${version}

wsAppDir=$baseDir/applications/unireg-ws
wsDeployDir=$baseDir/app/unireg-ws/${version}

nexusAppDir=$baseDir/applications/unireg-nexus
nexusDeployDir=$baseDir/app/unireg-nexus/${version}

webFileOrig=unireg-web-release.zip
wsFileOrig=unireg-ws-release.zip
nexusFileOrig=unireg-nexus-release.zip

#########

function compile_all() {

  if [ $DEPLOY_ONLY == 0 ]; then
	  (cd base && mvn -Pnot,oracle,ext,jspc clean install)
  fi
  if [ $? != 0 ]; then
	  echo "!!! Erreur lors du build" >&2
	  exit 1
  fi
}

function assemble_app() {
  local appName=$1

  if [ $DEPLOY_ONLY == 0 ]; then
	  (cd $appName && mvn -Pnot,oracle,jspc assembly:assembly)
  fi
  if [ $? != 0 ]; then
	  echo "!!! Erreur lors de l'assembly de $appName" >&2
	  exit 1
  fi
}

function copy_with_timestamp() {
  local inputFile=$1
  local dirFile=$(dirname $inputFile)
  local outputFile=$(basename $inputFile .zip)-${version}-${DATE}.zip

  cp $inputFile $dirFile/$outputFile
  echo $dirFile/$outputFile
}

function deploy_app() {
  local zipFilepath=$1
  local appDir=$2
  local deployDir=$3
  local zipFilename=$(basename $zipFilepath)

  scp $zipFilepath $user:$upDir/

  ssh $user "rm -rf $upDir/explode && mkdir -p $upDir/explode"
  ssh $user "cd $upDir/explode && unzip $upDir/$zipFilename"

  # copie des fichiers de config
  ssh $user "mkdir -p $appDir/config"
  ssh $user "config=\$(find ${upDir}/explode -name config -type d); cp \${config}/${env}/* $appDir/config"

  # copie du war
  ssh $user "mkdir -p $deployDir/deployment"
  ssh $user "depl=\$(find ${upDir}/explode -name deployment -type d); cp \${depl}/*.war $deployDir/deployment/"

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
webFileDest=$(copy_with_timestamp web/target/$webFileOrig)
wsFileDest=$(copy_with_timestamp ws/target/$wsFileOrig)
nexusFileDest=$(copy_with_timestamp nexus/target/$nexusFileOrig)

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
