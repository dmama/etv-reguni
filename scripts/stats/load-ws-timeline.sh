#! /bin/bash -
# prend un fichier de log applicatif unireg (unireg-ws.log) sur l'entrée standard
# et sort des lignes pour les moyennes de charges des web-sercvices - moyenne des cinq dernières minutes

WS=$1
if [ -z "$WS" ]; then
	echo "Syntaxe : $(basename "$0") WSName où WSName est le nom du web-service à regarder" >&2
	echo "--------" >&2
	echo "Les web-services connus sont :" >&2
	sed -e '/^ Caches   /,/^ Load\b/ D' | sed -e '/^ [[:alpha:]]/ !D' | awk '{ print $1; }' | sort -u | while read WS; do
		echo -e "\t$WS" >&2
	done
	exit 1
fi

sed -e '/^ Caches   /,/^ Load\b/ D' -e '/Statistiques des caches et services/ s/^/ /' | sed -e '/^ [[:alpha:]\[]/ !D' | grep "\(INFO\|$WS\)" | awk "/INFO/ { print \$3 \" \" \$4; } /$WS/ { print \$5; }" | sed -e '/:/ N' -e 's/\n/;/' -e 's/[^a-zA-Z0-9:;. -]//g'
