#!/bin/bash

dir=lib
cp=ubr.jar
for i in $(ls $dir); do
	cp=$cp:$dir/$i
done

#echo "CLasspath: $cp"
java -cp $cp ch.vd.uniregctb.ubr.BatchRunnerApp $@
