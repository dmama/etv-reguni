#! /bin/bash -
# génère les scripts SQL qui permettent d'annuler un tiers

if [ $# -ne 2 ]; then
	echo "Syntaxe : $(basename "$0") <no-jira> <no-ctb>" >&2
	exit 1
fi

TABLE_PREFIX_FILE=$(dirname "$0")/table-prefix
TABLE_PREFIX=""
if [ -e "$TABLE_PREFIX_FILE" -a -r "$TABLE_PREFIX_FILE" ]; then
        TABLE_PREFIX=$(< "$TABLE_PREFIX_FILE")
fi

JIRA=$1
CTB=$(echo "$2" | sed -e 's/\.//g')

if [[ ! "$JIRA" =~ ^[0-9]+$ ]]; then
	echo "Le numéro de cas Jira devrait être un nombre (trouvé : '$JIRA')" >&2
	exit 1
elif [[ ! "$CTB" =~ ^[0-9]+$ ]]; then
	echo "Le numéro de contribuable devrait être un nombre (trouvé : '$CTB')" >&2
	exit 1
fi

echo "-- Annulation du tiers $CTB"
echo "UPDATE ${TABLE_PREFIX}TIERS SET ANNULATION_DATE=CURRENT_DATE, ANNULATION_USER='SQL-SIFISC-$JIRA', LOG_MDATE=CURRENT_DATE, LOG_MUSER='SQL-SIFISC-$JIRA', INDEX_DIRTY=1 WHERE NUMERO=$CTB AND ANNULATION_DATE IS NULL;"
echo

echo "-- Annulation de ses rapports entre tiers et de ses tâches en instance, et de ses fors"
echo "UPDATE ${TABLE_PREFIX}RAPPORT_ENTRE_TIERS SET ANNULATION_DATE=CURRENT_DATE, ANNULATION_USER='SQL-SIFISC-$JIRA', LOG_MDATE=CURRENT_DATE, LOG_MUSER='SQL-SIFISC-$JIRA' WHERE (TIERS_SUJET_ID=$CTB OR TIERS_OBJET_ID=$CTB) AND ANNULATION_DATE IS NULL;"
echo "UPDATE ${TABLE_PREFIX}TACHE SET ANNULATION_DATE=CURRENT_DATE, ANNULATION_USER='SQL-SIFISC-$JIRA', LOG_MDATE=CURRENT_DATE, LOG_MUSER='SQL-SIFISC-$JIRA' WHERE CTB_ID=$CTB AND ETAT='EN_INSTANCE' AND ANNULATION_DATE IS NULL;"
echo "UPDATE ${TABLE_PREFIX}FOR_FISCAL SET ANNULATION_DATE=CURRENT_DATE, ANNULATION_USER='SQL-SIFISC-$JIRA', LOG_MDATE=CURRENT_DATE, LOG_MUSER='SQL-SIFISC-$JIRA' WHERE TIERS_ID=$CTB AND ANNULATION_DATE IS NULL;"
