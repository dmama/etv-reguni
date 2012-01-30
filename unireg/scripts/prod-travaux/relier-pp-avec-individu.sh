#! /bin/bash -
# génère les scripts SQL qui permettent de relier une personne physique avec un individu (passage en habitant)

if [ $# -ne 3 ]; then
	echo "Syntaxe : $(basename "$0") <no-jira> <no-ctb-pp> <no-ind-pp>" >&2
	exit 1
fi

JIRA=$1
CTB_PP=$(echo "$2" | sed -e 's/\.//g')
IND=$3

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

echo "-- Association de la personne physique $CTB_PP avec l'individu = $IND"
echo "UPDATE TIERS SET LOG_MDATE=CURRENT_DATE, LOG_MUSER='SQL-SIFISC-$JIRA', PP_HABITANT=1, NUMERO_INDIVIDU=$IND, INDEX_DIRTY=1 WHERE NUMERO=$CTB_PP;"
echo "DELETE FROM IDENTIFICATION_PERSONNE WHERE NON_HABITANT_ID=$CTB_PP;"
