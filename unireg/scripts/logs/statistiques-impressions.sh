#! /bin/bash -
# Calcule les statistiques des impressions locales (nombre de demandes, nombre d'échecs)

ENVIRONMENT=$1
if [ -z "$ENVIRONMENT" ]; then
        echo "Syntaxe : $(basename "$0") <env> avec <env> l'un de PR, VA, PP, TE" >&2
        exit 1
elif [[ ! "$ENVIRONMENT" =~ ^(PR|VA|PP|TE)$ ]]; then
        echo "Pour l'environnement, seuls PR, VA, PP et TE sont acceptés (trouvé : '$ENVIRONMENT')" >&2
        exit 1
fi

# si la méthode est directement appelée en ligne de commande (avec un tty en sortie), on n'encode pas particulièrement
# le flux en sortie ; en revanche, si le flux de sortie n'est pas un tty (appel depuis cron), on encode en ISO-8859-1
# pour que le mail soit bien interprété par Notes...
function encode_output() {
        if [ -t 1 ]; then
                cat -
        else
                iconv -t iso88591
        fi
}

# extrait le contenu du fichier (décompressé au besoin) sur la sortie standard
function dump_file() {
	local FILE=$1
	if [[ "$FILE" =~ \.xz$ ]]; then
		xzcat "$FILE"
	else
		cat "$FILE"
	fi
}

# fonction qui prend deux paramètres : le nom du fichier et l'intitulé de la date
function compute_stats() {
	local LOG_FILE=$1
	local DATE_LABEL=$2
	local IMP=$(dump_file "$LOG_FILE" | grep "\(docID:\|\[AUDIT\] Impression\)" -c)
	local LAST_IMP=$(dump_file "$LOG_FILE" | grep "\(docID:\|\[AUDIT\] Impression\)" | sed -e '$! D' -e 's/[^0-9: \.-]//g' | awk '{ print $2; }')
	local FAIL=$(dump_file "$LOG_FILE" | grep "EditiqueCommunicationException" -c)
	local LAST_FAIL=$(dump_file "$LOG_FILE" | grep -B 1 "EditiqueCommunicationException" | sed -e '$ D' | sed -e '$! D' -e 's/[^0-9: \.-]//g' | awk '{ print $2; }')
	local FIRST_FAIL=$(dump_file "$LOG_FILE" | grep -B 1 "EditiqueCommunicationException" | sed -e '2,$ D' -e 's/[^0-9: \.-]//g' | awk '{ print $2; }')
	echo -n "$DATE_LABEL : $FAIL échec(s) sur $IMP demande(s) d'impression"
	if [ "$IMP" -ne 0 ]; then
		local PERCENT_FAILURE=$(($FAIL * 100 / $IMP))
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

ls -1 unireg-web-cat_unireg${ENVIRONMENT}01.log.* | grep "[0-9]\+\(\.xz\)\?$" | tail -n 10 | while read FILE; do
	DATE=$(echo "$FILE" | sed -e 's/^.*log//' -e 's/[^0-9]//g')
	compute_stats "$FILE" "$DATE"
done | encode_output

if [ -e "unireg-web-cat_unireg${ENVIRONMENT}01.log" ]; then
	compute_stats "unireg-web-cat_unireg${ENVIRONMENT}01.log" "courant "
fi | encode_output
