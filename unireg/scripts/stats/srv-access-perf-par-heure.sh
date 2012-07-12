#! /bin/bash -
# on va essayer de sortir, par utilisateur méthode le temps moyen de réponse par heure

# [ServiceSecurite   ] INFO  [2011-01-23 00:28:54.675] [ocessor3] (68743 ms) getCollectivitesUtilisateur{visaOperateur=zsiizg}
# [ServiceSecurite   ] INFO  [2011-01-23 00:28:55.633] [ocessor3] (955 ms) getProfileUtilisateur{visaOperateur=zsiizg, codeCollectivite=22}
# [ServiceCivil      ] INFO  [2011-01-23 00:31:54.777] [Worker-9] (668 ms) getIndividu{noIndividu=988890, annee=2400, parties=null}
# [ServiceCivil      ] INFO  [2011-01-23 00:31:55.252] [Worker-9] (473 ms) getIndividu{noIndividu=183991, annee=2400, parties=null}
# [ServiceCivil      ] INFO  [2011-01-23 00:31:55.660] [Worker-9] (406 ms) getIndividu{noIndividu=988890, annee=2400, parties=[ADRESSES]}
# [ServiceInfra      ] INFO  [2011-01-23 00:31:56.130] [Worker-9] (437 ms) getPays{numeroOFS=8100}
# [ServiceInfra      ] INFO  [2011-01-23 00:31:58.228] [Worker-9] (2078 ms) getOfficeImpotDeCommune{noCommune=5591}
# [ServiceInfra      ] INFO  [2011-01-23 00:31:58.272] [Worker-9] (30 ms) getCollectivite{noColAdm=7}
# [ServiceInfra      ] INFO  [2011-01-23 00:31:58.308] [Worker-9] (24 ms) getOfficeImpot{noColAdm=7}
# [ServiceInfra      ] INFO  [2011-01-23 00:32:00.067] [Worker-9] (1738 ms) getCommunes

FILE="$1"
SRV="$2"
if [ -z "$FILE" ]; then
        echo "Syntaxe : $(basename "$0") srv-access.log nom-méthode" >&2
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

SERVICES=$(cat_file "$FILE" | sed -e 's/[^a-zA-Z0-9 :.={}-]\+//g' -e 's/{.*}//' | awk '{ print $8; }' | sort -u)
if [ -z "$SERVICES" ]; then
        echo "Aucune méthode trouvée (fichier vide ou mauvais fichier ?)" >&2
        exit 1
fi
if [ -z "$SRV" ]; then
        NB_SERVICES=$(echo "$SERVICES" | wc -l)
        if [ "$NB_SERVICES" -gt 1 ]; then
                IDX=1
                echo "$SERVICES" | while read LINE; do
                        echo -e "\t$IDX:\t$LINE"
                        ((++IDX))
                done >&2

                CHOSEN=""
                while [[ ! "$CHOSEN" =~ ^[1-9][0-9]*$ || "$CHOSEN" -gt "$NB_SERVICES" ]]; do
                        read -p "Quel est la méthode que l'on veut étudier ici? (1..$NB_SERVICES) : " CHOSEN;
                done

                SERVICE=$(echo "$SERVICES" | sed -e "$CHOSEN !D")
                echo "Méthode choisie : $SERVICE" >&2
        else
                echo "Une seule méthode trouvée : $SERVICES" >&2
                SERVICE="$SERVICES"
        fi
else
        SERVICE=$(echo "$SERVICES" | grep "^$SRV\$")
        if [ -z "$SERVICE" ]; then
                echo "Méthode '$SRV' inconnue, les services existants sont $(echo "$SERVICES" | xargs | sed -e 's/ /,/g')" >&2
                exit 1
        fi
fi

OLD_TS=""
COUNT=0
SUM_TIMES=0
MIN_TIME=99999999999999
MAX_TIME=-1
echo "TIMESTAMP;AVG_TIME;COUNT;MIN_TIME;MAX_TIME"
while read TS_DAY TS_HOUR TIME; do

	TS="$TS_DAY $TS_HOUR"
	if [ "$OLD_TS" != "$TS" ]; then
		if [ -n "$OLD_TS" ]; then
			echo "$OLD_TS;$(($SUM_TIMES / $COUNT));$COUNT;$MIN_TIME;$MAX_TIME"
			COUNT=0
			SUM_TIMES=0
			MIN_TIME=99999999999999
			MAX_TIME=-1
		fi
		OLD_TS="$TS"
	fi
	((++COUNT))
	((SUM_TIMES+=TIME))
	if [ -z "$MIN_TIME" -o "$TIME" -lt "$MIN_TIME" ]; then MIN_TIME=$TIME; fi
	if [ -z "$MAX_TIME" -o "$TIME" -gt "$MAX_TIME" ]; then MAX_TIME=$TIME; fi

done < <(cat_file "$FILE" | sed -e 's/[^a-zA-Z0-9 :.={}-]\+//g' -e 's/{.*}//' | awk " \$8 ~ /^$SERVICE\$/ { print \$3 \" \" \$4 \" \" \$6; }" | sed -e 's/-/\//g' -e 's/[^0-9\.: /]//g' -e 's/:[0-9]\+:[0-9]\+\.[0-9]\+//')

if [ "$COUNT" -gt 0 ]; then
	echo "$OLD_TS;$(($SUM_TIMES / $COUNT));$COUNT;$MIN_TIME;$MAX_TIME"
fi
