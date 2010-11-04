#! /bin/bash -
# on va essayer de sortir, par utilisateur [toto] et méthode le temps moyen de réponse par minute

# [tiers2.read] INFO  [2010-10-14 07:10:56.625] [web-it] (0 ms) GetTiersHisto{login=UserLogin{userId='zaizzt', oid=22}, tiersNumber=38709906, parts=[COMPOSANTS_MENAGE, FORS_GESTION, ADRESSES_ENVOI, PERIODE_IMPOSITION, FORS_FISCAUX, ADRESSES, ASSUJETTISSEMENTS, SITUATIONS_FAMILLE, DECLARATIONS]}
# [tiers2.read] INFO  [2010-10-14 07:10:56.648] [web-it] (0 ms) GetTiersHisto{login=UserLogin{userId='zaizzt', oid=22}, tiersNumber=10031172, parts=[COMPOSANTS_MENAGE, FORS_GESTION, ADRESSES_ENVOI, PERIODE_IMPOSITION, FORS_FISCAUX, ADRESSES, ASSUJETTISSEMENTS, SITUATIONS_FAMILLE, DECLARATIONS]}
# [tiers2.read] INFO  [2010-10-14 07:10:58.816] [web-it] (1598 ms) GetTiersHisto{login=UserLogin{userId='zaizzt', oid=22}, tiersNumber=52902307, parts=[COMPOSANTS_MENAGE, FORS_GESTION, ADRESSES_ENVOI, PERIODE_IMPOSITION, FORS_FISCAUX, ADRESSES, ASSUJETTISSEMENTS, SITUATIONS_FAMILLE, DECLARATIONS]}
# [tiers2.read] INFO  [2010-10-14 07:10:58.969] [web-it] (1719 ms) GetTiersHisto{login=UserLogin{userId='zaizzt', oid=22}, tiersNumber=59108001, parts=[COMPOSANTS_MENAGE, FORS_GESTION, ADRESSES_ENVOI, PERIODE_IMPOSITION, FORS_FISCAUX, ADRESSES, ASSUJETTISSEMENTS, SITUATIONS_FAMILLE, DECLARATIONS]}
# [tiers2.read] INFO  [2010-10-14 07:10:59.117] [web-it] (3231 ms) GetTiersHisto{login=UserLogin{userId='zaizzt', oid=22}, tiersNumber=68842404, parts=[COMPOSANTS_MENAGE, FORS_GESTION, ADRESSES_ENVOI, PERIODE_IMPOSITION, FORS_FISCAUX, ADRESSES, ASSUJETTISSEMENTS, SITUATIONS_FAMILLE, DECLARATIONS]}
# [tiers2.read] INFO  [2010-10-14 07:10:59.205] [web-it] (3344 ms) GetTiersHisto{login=UserLogin{userId='zaizzt', oid=22}, tiersNumber=16906208, parts=[COMPOSANTS_MENAGE, FORS_GESTION, ADRESSES_ENVOI, PERIODE_IMPOSITION, FORS_FISCAUX, ADRESSES, ASSUJETTISSEMENTS, SITUATIONS_FAMILLE, DECLARATIONS]}
# [tiers2.read] INFO  [2010-10-14 07:10:59.811] [web-it] (1440 ms) GetTiersHisto{login=UserLogin{userId='zaizzt', oid=22}, tiersNumber=17702301, parts=[COMPOSANTS_MENAGE, FORS_GESTION, ADRESSES_ENVOI, PERIODE_IMPOSITION, FORS_FISCAUX, ADRESSES, ASSUJETTISSEMENTS, SITUATIONS_FAMILLE, DECLARATIONS]}
# [tiers2.read] INFO  [2010-10-14 07:11:00.534] [web-it] (0 ms) GetTiersHisto{login=UserLogin{userId='zaizzt', oid=22}, tiersNumber=41229808, parts=[COMPOSANTS_MENAGE, FORS_GESTION, ADRESSES_ENVOI, PERIODE_IMPOSITION, FORS_FISCAUX, ADRESSES, ASSUJETTISSEMENTS, SITUATIONS_FAMILLE, DECLARATIONS]}
# [tiers2.read] INFO  [2010-10-14 07:11:01.835] [web-it] (0 ms) GetTiersHisto{login=UserLogin{userId='zaizzt', oid=22}, tiersNumber=41229808, parts=[COMPOSANTS_MENAGE, FORS_GESTION, ADRESSES_ENVOI, PERIODE_IMPOSITION, FORS_FISCAUX, ADRESSES, ASSUJETTISSEMENTS, SITUATIONS_FAMILLE, DECLARATIONS]}
# [tiers2.read] INFO  [2010-10-14 07:11:02.108] [web-it] (1915 ms) GetTiersHisto{login=UserLogin{userId='zaizzt', oid=22}, tiersNumber=33007803, parts=[COMPOSANTS_MENAGE, FORS_GESTION, ADRESSES_ENVOI, PERIODE_IMPOSITION, FORS_FISCAUX, ADRESSES, ASSUJETTISSEMENTS, SITUATIONS_FAMILLE, DECLARATIONS]}

FILE="$1"
WS_USER="$2"
SRV="$3"
if [ -z "$FILE" -o -z "$WS_USER" ]; then
        echo "Syntaxe : $(basename "$0") ws-access.log <nom-utilisaeur-ws> [nom méthode ws]" >&2
        exit 1
elif [ ! -r "$FILE" -o ! -f "$FILE" ]; then
        echo "Fichier $FILE inaccessible" >&2
        exit 1
fi

function cat_file {
	FILE="$1"
	if [[ "$FILE" =~ lzma$ ]]; then
		lzcat "$FILE"
	else
		cat "$FILE"
	fi
}

SERVICES=$(cat_file "$FILE" | grep "\[$WS_USER\]" | awk '{ print $8; }' | sed -e 's/{.*$//' | sort | uniq)
if [ -z "$SERVICES" ]; then
	USERS=$(cat_file "$FILE" | awk '{ print $5; }' | sort | uniq | sed -e 's/^\[\(.*\)\]$/\1/' | xargs | sed -e 's/ /,/g')
	echo "Aucune méthode trouvée appelée par l'utilisateur web-service '$WS_USER' (utilisateurs trouvés : $USERS)" >&2
	exit 1
fi
if [ -z "$SRV" ]; then
        NB_SERVICES=$(echo "$SERVICES" | wc -l)
	if [ "$NB_SERVICES" -gt 1 ]; then
	        IDX=1
	        echo "$SERVICES" | while read LINE; do
	                echo -e "\t$IDX:\t$LINE"
	                ((++IDX))
		done
	
		CHOSEN=""
	        while [[ ! "$CHOSEN" =~ ^[1-9][0-9]*$ || "$CHOSEN" -gt "$NB_SERVICES" ]]; do
	                read -p "Quel est le service que l'on veut étudier ici? (1..$NB_SERVICES) : " CHOSEN;
		done
	
	        SERVICE=$(echo "$SERVICES" | sed -e "$CHOSEN !D")
		echo "Service choisi : $SERVICE"
	else
		echo "L'utilisateur '$WS_USER' n'a jamais appelé qu'une seule méthode du web-service : $SERVICES"
		SERVICE="$SERVICES"
	fi
else
        SERVICE=$(echo "$SERVICES" | grep "^$SRV\$")
        if [ -z "$SERVICE" ]; then
                echo "Service '$SRV' inconnu, les services existants sont $(echo "$SERVICES" | xargs | sed -e 's/ /,/g')" >&2
                exit 1
        fi
fi

OLD_TS=""
COUNT=0
SUM_TIMES=0
while read TS TIME; do

	if [ "$OLD_TS" != "$TS" ]; then
		if [ -n "$OLD_TS" ]; then
			echo "$OLD_TS;$(($SUM_TIMES / $COUNT))"
			COUNT=0
			SUM_TIMES=0
		fi
		OLD_TS="$TS"
	fi
	((++COUNT))
	((SUM_TIMES+=TIME))

done < <(cat_file "$FILE" | grep "\[$WS_USER\]" | awk " \$8 ~ /^$SERVICE\{/ { print \$3 \" \" \$4 \" \" \$6; }" | sed -e 's/[^0-9\.: ]//g' -e 's/ /-/' -e 's/:[0-9]\+\.[0-9]\+//')

if [ "$COUNT" -gt 0 ]; then
	echo "$OLD_TS;$(($SUM_TIMES / $COUNT))"
fi
