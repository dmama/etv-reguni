#! /bin/bash -
# génère les scripts SQL qui permettent de changer une PersonnePhysique en MenageCommun

if [ $# -ne 4 -a $# -ne 5 ]; then
	echo "Syntaxe : $(basename "$0") <no-jira> <no-ctb-mc> <date-ouv-rapport-app-ménage> <no-ctb-principal> [<no-ctb-secondaire>]" >&2
	exit 1
fi

JIRA=$1
MC=$(echo "$2" | sed -e 's/\.//g')
OUVERTURE=$3
CTB_PRINC=$(echo "$4" | sed -e 's/\.//g')
CTB_CONJ=$(echo "$5" | sed -e 's/\.//g')

if [[ ! "$JIRA" =~ ^[0-9]+$ ]]; then
	echo "Le numéro de cas Jira devrait être un nombre (trouvé : '$JIRA')" >&2
	exit 1
elif [[ ! "$MC" =~ ^[0-9]+$ ]]; then
	echo "Le numéro de contribuable ménage commun devrait être un nombre (trouvé : '$MC')" >&2
	exit 1
elif [[ ! "$OUVERTURE" =~ ^[0-9]+$ ]]; then
	echo "La date d'ouverture devrait être un nombre YYYYMMDD (trouvé : '$OUVERTURE')" >&2
	exit 1
elif [ ${#OUVERTURE} -ne 8 -o $OUVERTURE -lt 19200000 -o $OUVERTURE -gt $(date +"%Y%m%d") ]; then
	echo "La date d'ouverture se semble pas être au format YYYYMMDD (trouvé : '$OUVERTURE')" >&2
	exit 1
elif [[ ! "$CTB_PRINC" =~ ^[0-9]+$ ]]; then
	echo "Le numéro de contribuable personne physique 'principal' dans le couple devrait être un nombre (trouvé : '$CTB_PRINC')" >&2
	exit 1
elif [ -n "$CTB_CONJ" ]; then
	if [[ ! "$CTB_CONJ" =~ ^[0-9]+$ ]]; then
		echo "Le numéro de contribuable personne physique 'conjoint' dans le couple devrait être un nombre (trouvé : '$CTB_CONJ')" >&2
		exit 1
	fi
fi

echo "-- Création des rapports entre tiers de type 'appartenance ménage'"
echo "INSERT INTO RAPPORT_ENTRE_TIERS (RAPPORT_ENTRE_TIERS_TYPE, ID, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, DATE_DEBUT, TIERS_SUJET_ID, TIERS_OBJET_ID)"
echo "SELECT 'AppartenanceMenage', HIBERNATE_SEQUENCE.NEXTVAL, CURRENT_DATE, 'SQL-$JIRA', CURRENT_DATE, 'SQL-$JIRA', $OUVERTURE, NUMERO, $MC"
if [ -n "$CTB_CONJ" ]; then
	echo "FROM TIERS WHERE NUMERO IN ($CTB_PRINC, $CTB_CONJ);"
else 
	echo "FROM TIERS WHERE NUMERO=$CTB_PRINC;"
fi
