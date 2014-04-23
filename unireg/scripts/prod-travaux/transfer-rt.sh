#! /bin/bash -
# génère les scripts SQL qui permettent de transférer les rapports de travail d'un tiers à un autre

if [ $# -ne 3 ]; then
	echo "Syntaxe : $(basename "$0") <no-jira> <no-ctb-src> <no-ctb-dest>" >&2
	exit 1
fi

JIRA=$1
SRC=$(echo "$2" | sed -e 's/\.//g')
DEST=$(echo "$3" | sed -e 's/\.//g')

TABLE_PREFIX_FILE=$(dirname "$0")/table-prefix
TABLE_PREFIX=""
if [ -e "$TABLE_PREFIX_FILE" -a -r "$TABLE_PREFIX_FILE" ]; then
	TABLE_PREFIX=$(< "$TABLE_PREFIX_FILE")
fi

if [[ ! "$JIRA" =~ ^[0-9]+$ ]]; then
	echo "Le numéro de cas Jira devrait être un nombre (trouvé : '$JIRA')" >&2
	exit 1
elif [[ ! "$SRC" =~ ^[0-9]+$ ]]; then
	echo "Le numéro de contribuable source devrait être un nombre (trouvé : '$SRC')" >&2
	exit 1
elif [[ ! "$DEST" =~ ^[0-9]+$ ]]; then
	echo "Le numéro de contribuable destination devrait être un nombre (trouvé : '$DEST')" >&2
	exit 1
fi

echo "-- Déplacement des rapports de travail du contribuable $SRC vers le contribuable $DEST"
echo "UPDATE ${TABLE_PREFIX}RAPPORT_ENTRE_TIERS SET LOG_MUSER='SQL-SIFISC-$JIRA', LOG_MDATE=CURRENT_DATE, TIERS_SUJET_ID=$DEST"
echo "WHERE TIERS_SUJET_ID=$SRC AND RAPPORT_ENTRE_TIERS_TYPE='RapportPrestationImposable';"
