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


LVERSION=$(grep "long=" unireg/base/version.txt | awk -F= '{ print $2; }')
SVERSION=$(grep "short=" unireg/base/version.txt | awk -F= '{ print $2; }')
echo "$LVERSION / $SVERSION"

user=beauniregi@solve61v
appDir=ch_vd_appDir/form-ctbv1
relFileOrig=uniregctb-release.zip
relFileDest=uniregctb-release-$LVERSION-${DATE}.zip
env=formation


full=1
if [ "$1x" == "deployx" ];then
	full=0
fi
echo "Full: $full"




if [ $full == 1 ]; then
	(cd unireg/base && mvn -Pnot,env.form,oracle clean install)
	if [ $? != 0 ]; then
		echo "!!! Erreur lors du build"
		exit 1
	fi
fi

if [ $full == 1 ]; then
	(cd unireg/web && mvn -Pnot,env.form,oracle assembly:assembly)
	if [ $? != 0 ]; then
		echo "!!! Erreur lors du build"
		exit 1
	fi
fi


# Cleanup
# !!! Attention de ne pas effacer l'index Lucene dans $appDir/lucene/ !!!
ssh $user "mkdir $appDir ; rm $appDir/*" # Efface les fichiers .zip
ssh $user "rm -R $appDir/config/* $appDir/deployment/* $appDir/docs/* $appDir/libs/* $appDir/sql/*"


# Deploiement
cp -v unireg/web/target/$relFileOrig unireg/web/target/$relFileDest
scp unireg/web/target/$relFileDest $user:$appDir/
ssh $user "cd $appDir && unzip $relFileDest"
ssh $user "cd $appDir/config && cp $env/* ."


# Notifie Weblogic d'updater l'application
echo "Debut du deploiement at: $(date)"
(cd unireg/web && ant deploy.form)


echo "Fin du deploiement at: $(date)"

