#! /bin/bash -
# prend un fichier de log applicatif unireg (unireg-web.log ou unireg-ws.log) sur l'entrée standard (ou la ligne de commande)
# et sort des lignes pour les temps d'accès aux service civil (getIndividu et getIndividus) - moyenne des cinq dernières minutes

echo "TIMESTAMP;getIndividu;getIndividus"

sed -e '/Statistiques des caches et services/,/nexus/{!d;$d}' -- "$@" | awk '/logStats/ { print $3 " " $4; } / - getIndividus/ { print $2 " " $7; } / - getIndividu / { print $2 " " $4; }' | sed -e '/getIndividu/ s/[^0-9a-zA-Z ]//g' -e '/[0-9]/ !D' | tac | sed -e '/get/ N;s/\n/;/' -e '/get.*[0-9]$/ N;s/\n/;/' | sed -e '/get/!D' | tac | sed -e 's/[^0-9a-zA-Z :.;-]//g' -e '/get.*get/! s/^/;/' -e 's/getIndividu[s ]\+//g' | sed -e 's/;0;/;;/' -e 's/;0$/;/' | while IFS=";" read PL SG TS; do

	echo "$TS;$SG;$PL"

done
