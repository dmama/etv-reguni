#! /bin/bash -
# génère les scripts SQL qui permettent d'aligner les événements civils avec les personnes physiques

if [ $# -ne 3 ]; then
	echo "Syntaxe : $(basename "$0") <no-jira> <no-ctb-pp> <no-ind-pp>" >&2
	exit 1
fi

JIRA=$1
CTB_PP=$(echo "$2" | sed -e 's/\.//g')
IND=$3

TABLE_PREFIX_FILE=$(dirname "$0")/table-prefix
TABLE_PREFIX=""
if [ -e "$TABLE_PREFIX_FILE" -a -r "$TABLE_PREFIX_FILE" ]; then
        TABLE_PREFIX=$(< "$TABLE_PREFIX_FILE")
fi

if [[ ! "$JIRA" =~ ^[0-9]+$ ]]; then
	echo "Le numéro de cas Jira devrait être un nombre (trouvé : '$JIRA')" >&2
	exit 1
elif [[ ! "$CTB_PP" =~ ^[0-9]+$ ]]; then
	echo "Le numéro de contribuable personne physique devrait être un nombre (trouvé : '$CTB_PP')" >&2
	exit 1
elif [[ ! "$IND" =~ ^[0-9]+$ ]]; then
	echo "Le numéro d'individu pour la personne physique devrait être un nombre (trouvé : '$IND')" >&2
	exit 1
fi

echo "-- Association des événements civils de l'individu $IND avec la personne physique $CTB_PP"
echo "UPDATE ${TABLE_PREFIX}EVENEMENT_CIVIL SET LOG_MDATE=CURRENT_DATE, LOG_MUSER='SQL-SIFISC-$JIRA', HAB_PRINCIPAL=$CTB_PP WHERE NO_INDIVIDU_PRINCIPAL=$IND AND HAB_PRINCIPAL!=$CTB_PP;"
echo "UPDATE ${TABLE_PREFIX}EVENEMENT_CIVIL SET LOG_MDATE=CURRENT_DATE, LOG_MUSER='SQL-SIFISC-$JIRA', HAB_CONJOINT=$CTB_PP WHERE NO_INDIVIDU_CONJOINT=$IND AND HAB_CONJOINT!=$CTB_PP;"
