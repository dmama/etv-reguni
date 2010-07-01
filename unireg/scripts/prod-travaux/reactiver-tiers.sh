#! /bin/bash -
# génère les scripts SQL qui permettent de réactiver un tiers annulé à tort à une date donnée

if [ $# -ne 3 ]; then
	echo "Syntaxe : $(basename "$0") <no-jira> <no-ctb> <date-annulation>" >&2
	exit 1
fi

JIRA=$1
CTB=$(echo "$2" | sed -e 's/\.//g')
DATE_ANNULATION=$3

if [[ ! "$JIRA" =~ ^[0-9]+$ ]]; then
	echo "Le numéro de cas Jira devrait être un nombre (trouvé : '$JIRA')" >&2
	exit 1
elif [[ ! "$CTB" =~ ^[0-9]+$ ]]; then
	echo "Le numéro de contribuable personne physique devrait être un nombre (trouvé : '$CTB')" >&2
	exit 1
elif [[ ! "$DATE_ANNULATION" =~ ^[0-9]+$ ]]; then
	echo "La date d'annulation devrait être un nombre (trouvé : '$DATE_ANNULATION')" >&2
	exit 1
elif [ ${#DATE_ANNULATION} -ne 8 -o $DATE_ANNULATION -lt 19200000 -o $DATE_ANNULATION -gt $(date +"%Y%m%d") ]; then
        echo "La date d'annulation ne semble pas être au format YYYYMMDD (trouvé : '$DATE_ANNULATION')" >&2
        exit 1
fi

echo "-- Dé-annulation du contribuable $CTB"
echo "UPDATE TIERS SET ANNULATION_DATE=NULL, ANNULATION_USER=NULL, LOG_MDATE=CURRENT_DATE, LOG_MUSER='SQL-$JIRA', INDEX_DIRTY=1 WHERE NUMERO=$CTB AND ANNULATION_DATE IS NOT NULL;"
echo
echo "-- Réouverture des fors fermés pour annulation au $DATE_ANNULATION"
echo "UPDATE FOR_FISCAL SET LOG_MDATE=CURRENT_DATE, LOG_MUSER='SQL-$JIRA', DATE_FERMETURE=NULL, MOTIF_FERMETURE=NULL WHERE TIERS_ID=$CTB AND ANNULATION_DATE IS NULL AND DATE_FERMETURE=$DATE_ANNULATION AND MOTIF_FERMETURE='ANNULATION';"
