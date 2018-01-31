#! /bin/bash -
# Calcul du nombre d'appels moyen par jour et par contribuable (on se base exclusivement sur les appels unitaires - pas les GetBatch... - pour lesquels
# le nom du paramètre du numéro du tiers est tiersNumber - v2 - ou partyNumber - v3)

echo "DATE;NB_CTB_DIFFERENTS;NB_APPELS_TOTAL;MAX_APPELS_PAR_CTB;MOYENNE_APPELS_PAR_CTB"

# paramètres :
# 1. date
# 2. nombre de contribuables différents appelés ce jour-là
# 3. nombre total d'appels concernant ces contribuables
# 4. nombre maximal d'appels qu'a reçu un contribuable individuel
function dump() {
	MEAN=$(python -c "print $3.0/$2")
	echo "$1;$2;$3;$4;$MEAN"
}

NB_CALLS=0
NB_CTB=0
MAX_CALLS_PER_CTB=0
OLD_DATE=""
while read NB DATE CTB; do

	if [ "$DATE" != "$OLD_DATE" ]; then
		if [ "$NB_CALLS" -gt 0 ]; then
			dump "$OLD_DATE" "$NB_CTB" "$NB_CALLS" "$MAX_CALLS_PER_CTB"
		fi
		OLD_DATE="$DATE"
		NB_CTB=0
		NB_CALLS=0
		MAX_CALLS_PER_CTB=0
	fi
	((++NB_CTB))
	((NB_CALLS+=$NB))
	if [ "$MAX_CALLS_PER_CTB" -lt "$NB" ]; then
		MAX_CALLS_PER_CTB="$NB"
	fi

done < <(grep -h "\(tiersNumber\|partyNumber\)=" "$@" | awk '{ print $3 "\t" $0; }' | sed -e 's/^.\([^\t]\+\)\t.*\(tiersNumber\|partyNumber\)=\([0-9]\+\).*$/\1\t\3/' | sort | uniq -c)

if [ "$NB_CALLS" -gt 0 ]; then
	dump "$OLD_DATE" "$NB_CTB" "$NB_CALLS" "$MAX_CALLS_PER_CTB"
fi
