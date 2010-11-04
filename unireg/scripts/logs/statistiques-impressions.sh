#! /bin/bash -
# Calcule les statistiques des impressions locales (nombre de demandes, nombre d'échecs)

ENVIRONMENT=$1
if [ -z "$ENVIRONMENT" ]; then
        echo "Syntaxe : $(basename "$0") <env> avec <env> l'un de PR, PO, VA, PP, FO" >&2
        exit 1
elif [[ ! "$ENVIRONMENT" =~ ^(PR|PO|VA|PP|FO)$ ]]; then
        echo "Pour l'environnement, seuls PR, PO, VA, PP et FO sont acceptés (trouvé : '$ENVIRONMENT')" >&2
        exit 1
fi

# fonction qui prend deux paramètres : le nom du fichier et l'intitulé de la date
function compute_stats() {
	LOG_FILE=$1
	DATE_LABEL=$2
	IMP=$(grep "\(docID:\|\[AUDIT\] Impression\)" "$LOG_FILE" -c)
	LAST_IMP=$(grep "\(docID:\|\[AUDIT\] Impression\)" "$LOG_FILE" | sed -e '$! D' -e 's/[^0-9: \.-]//g' | awk '{ print $2; }')
	FAIL=$(grep "EditiqueCommunicationException" "$LOG_FILE" -c)
	LAST_FAIL=$(grep -B 1 "EditiqueCommunicationException" "$LOG_FILE" | sed -e '$ D' | sed -e '$! D' -e 's/[^0-9: \.-]//g' | awk '{ print $2; }')
	FIRST_FAIL=$(grep -B 1 "EditiqueCommunicationException" "$LOG_FILE" | sed -e '2,$ D' -e 's/[^0-9: \.-]//g' | awk '{ print $2; }')
	echo -n "$DATE_LABEL : $FAIL échec(s) sur $IMP demande(s) d'impression"
	if [ "$IMP" -ne 0 ]; then
		PERCENT_FAILURE=$(($FAIL * 100 / $IMP))
		echo -n " ($PERCENT_FAILURE% d'échec)"
		if [ "$FAIL" -ne 0 ]; then
			echo -n ", échec(s) entre $FIRST_FAIL et $LAST_FAIL"
		fi
		echo ", dernière demande à $LAST_IMP."
	else
		echo "."
	fi
}

# les fichiers sont téléchargés dans un sous-répertoire spécifique à l'environnement voulu
cd "$(dirname "$0")/$ENVIRONMENT/unireg-web"

ls -1 unireg-web.log.* | grep "[0-9]\+$" | while read FILE; do
	DATE=$(echo "$FILE" | sed -e 's/[^0-9]//g')
	compute_stats "$FILE" "$DATE"
done

if [ -e "unireg-web.log" ]; then
	compute_stats "unireg-web.log" "courant "
fi
