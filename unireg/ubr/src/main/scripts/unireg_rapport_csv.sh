#!/bin/bash

if [ -z "$JAVA_HOME" ]; then
	echo "Environment variable JAVA_HOME must be set!" >&2
	exit 1
elif [ ! -e "$JAVA_HOME/bin/java" ]; then
	echo "File $JAVA_HOME/bin/java cannot be found!" >&2
	exit 1
fi

basedir=$(dirname "$0")
cp=""
while read jar; do
	cp="$cp:${jar}"
done < <(find "${basedir}/../lib" -type f -name "*.jar")

#echo "Classpath: $cp"
$JAVA_HOME/bin/java -cp "$cp" ch.vd.uniregctb.rapport.RapportCsvExtractorApp "$@"
