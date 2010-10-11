#! /bin/bash -
# génère les scripts SQL qui permettent de changer une PersonnePhysique en MenageCommun

if [ $# -ne 4 -a $# -ne 5 ]; then
	echo "Syntaxe : $(basename "$0") <no-jira> <no-ctb-pp> <date-ouv-rapport-app-ménage> <no-ctb-principal> [<no-ctb-secondaire>]" >&2
	exit 1
fi

JIRA=$1
PP=$(echo "$2" | sed -e 's/\.//g')
OUVERTURE=$3
CTB_PRINC=$(echo "$4" | sed -e 's/\.//g')
CTB_CONJ=$(echo "$5" | sed -e 's/\.//g')

if [[ ! "$JIRA" =~ ^[0-9]+$ ]]; then
	echo "Le numéro de cas Jira devrait être un nombre (trouvé : '$JIRA')" >&2
	exit 1
elif [[ ! "$PP" =~ ^[0-9]+$ ]]; then
	echo "Le numéro de contribuable personne physique à transformer devrait être un nombre (trouvé : '$PP')" >&2
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

echo "-- Transformation de la personne physique $PP en ménage commun"
echo "UPDATE TIERS SET TIERS_TYPE='MenageCommun', LOG_MDATE=CURRENT_DATE, LOG_MUSER='SQL-$JIRA', PP_HABITANT=NULL, NUMERO_INDIVIDU=NULL, INDEX_DIRTY=1 WHERE NUMERO=$PP AND TIERS_TYPE='PersonnePhysique';"
echo "DELETE FROM SITUATION_FAMILLE WHERE CTB_ID=$PP OR TIERS_PRINCIPAL_ID=$PP;"
echo "DELETE FROM RAPPORT_ENTRE_TIERS WHERE TIERS_SUJET_ID=$PP AND RAPPORT_ENTRE_TIERS_TYPE='AppartenanceMenage';"
echo "DELETE FROM IDENTIFICATION_PERSONNE WHERE NON_HABITANT_ID=$PP;"
echo

$(dirname "$0")/add-rapport-appartenance-menage.sh $JIRA $PP $OUVERTURE $CTB_PRINC $CTB_CONJ

echo
echo "-- Recopie des droits d'accès de la personne physique $PP vers les membres du nouveau ménage"
echo "INSERT INTO DROIT_ACCESS (ID, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, DATE_DEBUT, DATE_FIN, NIVEAU, NUMERO_IND_OPER, TYPE, TIERS_ID)"
echo "SELECT HIBERNATE_SEQUENCE.NEXTVAL, CURRENT_DATE, 'SQL-$JIRA', CURRENT_DATE, 'SQL-$JIRA' DATE_DEBUT, DATE_FIN, NIVEAU, NUMERO_IND_OPER, TYPE, $CTB_PRINC"
echo "FROM DROIT_ACCESS DA WHERE DA.ANNULATION_DATE IS NULL AND DA.TIERS_ID=$PP;"
if [ -n "$CTB_CONJ" ]; then
	echo "INSERT INTO DROIT_ACCESS (ID, LOG_CDATE, LOG_CUSER, LOG_MDATE, LOG_MUSER, DATE_DEBUT, DATE_FIN, NIVEAU, NUMERO_IND_OPER, TYPE, TIERS_ID)"
	echo "SELECT HIBERNATE_SEQUENCE.NEXTVAL, CURRENT_DATE, 'SQL-$JIRA', CURRENT_DATE, 'SQL-$JIRA' DATE_DEBUT, DATE_FIN, NIVEAU, NUMERO_IND_OPER, TYPE, $CTB_CONJ"
	echo "FROM DROIT_ACCESS WHERE ANNULATION_DATE IS NULL AND TIERS_ID=$PP;"
fi
echo "UPDATE DROIT_ACCESS SET LOG_MDATE=CURRENT_DATE, LOG_MUSER='SQL-$JIRA', ANNULATION_DATE=CURRENT_DATE, ANNULATION_USER='SQL-$JIRA' WHERE TIERS_ID=$PP AND ANNULATION_DATE IS NULL;"
