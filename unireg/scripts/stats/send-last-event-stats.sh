#! /bin/bash -

if [ -z "$1" ]; then
	echo "Syntaxe : $(basename "$0") destinataire@domain.com ..." >&2
	exit 1
fi

# cherchons le dernier rapport d'exécution du batch de statistiques des événements
BASE_DIR=~/logs/PR/repository

function find_last_report() {
	find "$BASE_DIR" -type f -name "*RapportStatsEvenements.pdf" | while read FILE; do
		stat "$FILE" --printf "%Y\t%n\n"
	done | sort -nr | sed -e '2,$ D' -e 's/^.*\t//'
}

function mail_body() {
	echo "Bonjour,"
	echo
	echo "Vous voudrez bien trouver ci-joint les dernières statistiques des événements reçus pas l'application UNIREG en production."
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

# cherchons le dernier rapport d'exécution du batch de statistiques des événements
LAST_REPORT=$(find_last_report)
if [ -n "$LAST_REPORT" ]; then
	if [ -r "$LAST_REPORT" ]; then
		MODIF_TS=$(stat --format="%Y" "$LAST_REPORT")
		A_WEEK_AGO=$(date --date="7 days ago" +"%s")
		if [ "$MODIF_TS" -lt "$A_WEEK_AGO" ]; then
			echo "Le fichier $LAST_REPORT est vieux de plus d'une semaine... Il n'a donc pas été envoyé."
		else
			mail_body | mutt -a "$LAST_REPORT" -s "Statistiques des événements reçus par UNIREG en production" -- "$@"
			echo "Le fichier $LAST_REPORT a été envoyé à $(echo "$@" | xargs | sed -e 's/ /, /g')"
		fi
	else
		echo "Impossible de lire le fichier censé être envoyé : $LAST_REPORT"
	fi
fi | encode
