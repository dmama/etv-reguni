#!/bin/sh

# Ce script permet de restreindre l'accès à toutes les classes du package backport-util-concurrent à l'intérieur de Eclipse. Ce qui correspond plus-ou-moins au scope 'runtime' de Maven.

for file in `ls */.classpath`
do
  echo "Modifying" $file
  sed -i "s/backport-util-concurrent-3\.1-sources\.jar\">/backport-util-concurrent-3.1-sources.jar\">\n    <accessrules>\n      <accessrule kind=\"nonaccessible\" pattern=\"**\"\/>\n    <\/accessrules>/" $file
done