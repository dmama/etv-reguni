#! /bin/bash -

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

env=sipm
user=dsi_unireg@slv2984v.etat-de-vaud.ch

upDir=/ccv/data/dsi_unireg/uploads
baseDir=/ccv/data/dsi_unireg/uniregTE

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
	  (cd unireg/base && mvn -Pnot,oracle,ext,jspc clean install)
  fi
  if [ $? != 0 ]; then
	  echo "!!! Erreur lors du build" >&2
	  exit 1
  fi
}

function assemble_app() {
  local appName=$1

  if [ $DEPLOY_ONLY == 0 ]; then
	  (cd unireg/$appName && mvn -Pnot,oracle,jspc assembly:assembly)
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

  # modification du lieu de stockage des fichiers de log (pour l'accès avec logapp)
  local appName=$(basename $appDir)
  ssh $user "sed -e '/\bFile\b/ s/\([a-z-]\+\)\.log/\\\${ch.vd.projectDir}\/app\/$appName\/logs\/\1-\\\${ch.vd.serverName}.log/' -i $appDir/config/unireg-log4j.xml"

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
