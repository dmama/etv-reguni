#! /bin/bash -
# prend un fichier de log applicatif unireg (unireg-web.log ou unireg-ws.log) sur l'entrée standard (ou la ligne de commande)
# et sort des lignes pour les temps d'accès aux service civil (getIndividu et getIndividus) - moyenne des cinq dernières minutes

echo "TIMESTAMP;getIndividu;getIndividus"
sed -e '/Statistiques des caches et services/,/ - getIndividus/ !D' -- "$@" | grep "\(INFO\|Individu\)" | awk ' /INFO/ { print $3 " " $4; } / - getIndividu/ { print $2 " " $4; }' | sed -e '/:/ N;/:/ N' -e 's/\n/;/g' -e 's/[^a-zA-Z0-9:;. -]//g' | while IFS=";" read TS INFO1 INFO2; do

	IND=$(echo -e "$INFO1\n$INFO2" | grep "^getIndividu\b")
	INDS=$(echo -e "$INFO1\n$INFO2" | grep "^getIndividus\b")
	IFS=";" read IND INDS < <(echo "$IND;$INDS" | sed -e 's/[^0-9;]//g')
	echo "$TS;$IND;$INDS"

done
