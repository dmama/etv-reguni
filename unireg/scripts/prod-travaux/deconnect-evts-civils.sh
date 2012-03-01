#! /bin/bash -
# génère les scripts SQL qui permettent de déconnecter les événements civils reçus sur un doublon de ILF1

if [ $# -ne 2 ]; then
	echo "Syntaxe : $(basename "$0") <no-jira> <no-ind-doublon>" >&2
	exit 1
fi

JIRA=$1
IND=$2

TABLE_PREFIX_FILE=$(dirname "$0")/table-prefix
TABLE_PREFIX=""
if [ -e "$TABLE_PREFIX_FILE" -a -r "$TABLE_PREFIX_FILE" ]; then
        TABLE_PREFIX=$(< "$TABLE_PREFIX_FILE")
fi

if [[ ! "$JIRA" =~ ^[0-9]+$ ]]; then
	echo "Le numéro de cas Jira devrait être un nombre (trouvé : '$JIRA')" >&2
	exit 1
elif [[ ! "$IND" =~ ^[0-9]+$ ]]; then
	echo "Le numéro d'individu derait être un nombre (trouvé : '$IND')" >&2
	exit 1
fi

echo "-- Dé-connexion des événements civils sur l'ancien habitant (doublon ILF1)"
echo "UPDATE ${TABLE_PREFIX}EVENEMENT_CIVIL SET LOG_MDATE=CURRENT_DATE, LOG_MUSER='SQL-SIFISC-$JIRA', HAB_PRINCIPAL=NULL WHERE NO_INDIVIDU_PRINCIPAL=$IND;"
echo "UPDATE ${TABLE_PREFIX}EVENEMENT_CIVIL SET LOG_MDATE=CURRENT_DATE, LOG_MUSER='SQL-SIFISC-$JIRA', HAB_CONJOINT=NULL WHERE NO_INDIVIDU_CONJOINT=$IND;"
