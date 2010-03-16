
jarfile=jnotify-0.91.jar
group=net.contentobjects.jnotify
artifact=jnotify
version=0.91
#repo=inhouse
repo=public

mvn deploy:deploy-file -Dfile=$jarfile -DgroupId=$group -DartifactId=$artifact -Dversion=$version -Dpackaging=jar -Durl=http://calimero.etat-de-vaud.ch:9001/px-webapp/dav/$repo -DrepositoryId=$repo
