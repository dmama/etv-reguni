#!/bin/bash


#for file in `svn -R ls`  <-- marche pas à cause des espaces dans les noms de fichier
svn -R ls | while read file
do
    props=`svn proplist "$file" | grep mergeinfo`
    if [[ $props == *mergeinfo* ]]; then
#	echo "Removing property svn:mergeinfo from " $file
	svn propdel svn:mergeinfo "$file"
    fi
done
