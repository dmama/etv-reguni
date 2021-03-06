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

java -cp $classpath ch.vd.unireg.webservices.tiers2.perfs.PerfsClient $*
