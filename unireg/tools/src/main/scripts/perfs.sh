#!/bin/sh

classpath=""
for f in lib/*.jar
do
        if [ -n "$classpath" ]; then
                classpath="$classpath:$f"
        else
                classpath=$f
        fi
done

java -cp $classpath ch.vd.uniregctb.webservices.tiers.perfs.PerfsClient $*
