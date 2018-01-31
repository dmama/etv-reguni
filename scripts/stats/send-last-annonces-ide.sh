#! /bin/bash -

if [ "$#" -lt 2 ]; then
	echo "Syntaxe : $(basename "$0") threshold-days destinataire@domain.com ..." >&2
	exit 1
elif [[ ! "$1" =~ ^[1-9][0-9]*$ ]]; then
	echo "Le seuil en jours doit être un nombre entier positif" >&2
	exit 1
fi

THRESHOLD_DAYS=$1
shift 1

# cherchons le dernier rapport d'exécution du batch d'annonces à l'IDE
BASE_DIR=~/logs/PR/repository

function find_last_report() {
	find "$BASE_DIR" -type f -name "*RapportAnnoncesIDE.pdf" | while read FILE; do
		stat "$FILE" --printf "%Y\t%n\n"
	done | sort -nr | sed -e '2,$ D' -e 's/^.*\t//'
}

function mail_body() {
	echo "Bonjour,"
	echo
	echo "Vous voudrez bien trouver ci-joint le rapport d'exécution du job des annonces à l'IDE."
	echo "Bonne lecture !"
	echo
	echo "(ceci est un message automatique)"
}

# si la méthode est directement appelée en ligne de commande (avec un tty en sortie), on n'encode pas particulièrement
# le flux en sortie ; en revanche, si le flux de sortie n'est pas un tty (appel depuis cron), on encode en ISO-8859-1
# pour que le mail soit bien interprété par Notes...
function encode() {
	if [ -t 1 ]; then
		cat -
	else
		iconv -t iso88591
	fi
}

# cherchons le dernier rapport d'exécution du batch d'annonces à l'IDE
LAST_REPORT=$(find_last_report)
if [ -n "$LAST_REPORT" ]; then
	if [ -r "$LAST_REPORT" ]; then
		MODIF_TS=$(stat --format="%Y" "$LAST_REPORT")
		A_WEEK_AGO=$(date --date="$THRESHOLD_DAYS days ago" +"%s")
		if [ "$MODIF_TS" -lt "$A_WEEK_AGO" ]; then
			echo "Le fichier $LAST_REPORT est vieux de plus de $THRESHOLD_DAYS jour(s)... Il n'a donc pas été envoyé."
		else
			mail_body | mutt -a "$LAST_REPORT" -s "Rapport d'exécution du traitement des annonces à l'IDE en production" -- "$@"
			echo "Le fichier $LAST_REPORT a été envoyé à $(echo "$@" | xargs | sed -e 's/ /, /g')"
		fi
	else
		echo "Impossible de lire le fichier censé être envoyé : $LAST_REPORT"
	fi
fi | encode
