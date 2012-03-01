#! /bin/bash -
# génère les scripts SQL qui permettent de changer un MénageCommun en PersonnePhysique

if [ $# -ne 3 ]; then
	echo "Syntaxe : $(basename "$0") <no-jira> <no-ctb-mc> <no-ind-pp>" >&2
	exit 1
fi

JIRA=$1
MC=$(echo "$2" | sed -e 's/\.//g')
IND=$3

TABLE_PREFIX_FILE=$(dirname "$0")/table-prefix
TABLE_PREFIX=""
if [ -e "$TABLE_PREFIX_FILE" -a -r "$TABLE_PREFIX_FILE" ]; then
        TABLE_PREFIX=$(< "$TABLE_PREFIX_FILE")
fi

if [[ ! "$JIRA" =~ ^[0-9]+$ ]]; then
	echo "Le numéro de cas Jira devrait être un nombre (trouvé : '$JIRA')" >&2
	exit 1
elif [[ ! "$MC" =~ ^[0-9]+$ ]]; then
	echo "Le numéro de contribuable ménage devrait être un nombre (trouvé : '$MC')" >&2
	exit 1
elif [[ ! "$IND" =~ ^[0-9]+$ ]]; then
	echo "Le numéro d'individu de la personne physique résultante devrait être un nombre (trouvé : '$IND')" >&2
	exit 1
fi

echo "-- Transformation du ménage commun $MC en personne physique"
echo "UPDATE ${TABLE_PREFIX}TIERS SET TIERS_TYPE='PersonnePhysique', LOG_MDATE=CURRENT_DATE, LOG_MUSER='SQL-SIFISC-$JIRA', INDEX_DIRTY=1 WHERE NUMERO=$MC AND TIERS_TYPE='MenageCommun';"
echo "DELETE FROM ${TABLE_PREFIX}SITUATION_FAMILLE WHERE CTB_ID=$MC;"
echo "DELETE FROM ${TABLE_PREFIX}RAPPORT_ENTRE_TIERS WHERE TIERS_OBJET_ID=$MC AND RAPPORT_ENTRE_TIERS_TYPE='AppartenanceMenage';"
echo

$(dirname "$0")/relier-pp-avec-individu.sh $JIRA $MC $IND
