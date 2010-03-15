
version=1.0
date=$(date +%Y%m%d-%H%M%S)

dir=smsgw-$date-$version
mkdir $dir

mkdir $dir/config
mkdir $dir/logs
mkdir $dir/deployment
mkdir $dir/libs
mkdir $dir/docs
mkdir $dir/sql

cp target/smsgw-$version-SNAPSHOT.war $dir/deployment/smsgw.war
cp target/classes/log4j.xml $dir/config/
cp ../../12-Environnement/Deploiement.doc $dir/docs/
cp extlibs/com/ibm/db2/db2java-client.zip $dir/libs/
cp sql/*.sql $dir/sql/

zip -r $dir.zip $dir

