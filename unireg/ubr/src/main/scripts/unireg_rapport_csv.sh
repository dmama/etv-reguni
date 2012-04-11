#!/bin/bash

if [ -z "$JAVA_HOME" ]; then
	echo "Environment variable JAVA_HOME must be set!" >&2
	exit 1
elif [ ! -e "$JAVA_HOME/bin/java" ]; then
	echo "File $JAVA_HOME/bin/java cannot be found!" >&2
	exit 1
fi

dir=lib
cp=ubr.jar
for i in $(ls $dir); do
	cp=$cp:$dir/$i
done

#echo "CLasspath: $cp"
$JAVA_HOME/bin/java -cp "$cp" ch.vd.uniregctb.rapport.RapportCsvExtractorApp "$@"
