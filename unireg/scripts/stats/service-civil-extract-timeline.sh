#! /bin/bash -
# prend un fichier de log applicatif unireg (unireg-web.log ou unireg-ws.log) sur l'entrée standard
# et sort des lignes pour les temps d'accès aux service civil (getIndividu et getIndividus) - moyenne des cinq dernières minutes

echo "TIMESTAMP;getIndividu;getIndividus"
sed -e '/Statistiques des caches et services/,/ - getIndividus/ !D' | grep "\(INFO\|Individu\)" | awk ' /^\[unireg\]/ { print $3 " " $4; } /getIndividu/ { print $4; } ' | sed -e 's/[^0-9: .-]//g' -e '/:/ N' -e 's/\n/ /' -e '/:/ N' -e 's/\n/ /' -e 's/ /x/' -e 's/ /;/g' -e 's/x/ /'
