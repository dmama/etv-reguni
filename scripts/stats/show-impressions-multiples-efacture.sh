#! /bin/bash -

TMP_FILE=$(mktemp)
SRC_FILE="$1"
if [ ! -r "$SRC_FILE" ]; then
	echo "Le fichier '$SRC_FILE' n'est pas accessible en lecture !" >&2
	exit 1
fi

$(dirname "$0")/show-inscriptions-efacture.sh "$SRC_FILE" | grep "EN_ATTENTE" | awk -F";" '{ print $2; }' | sort | uniq -c | sort -n | awk '{ print $2 ";" $1; }' > "$TMP_FILE"

shift 1		# maintenant, les paramètres sont des adresses mails

function body() {
	echo "Nouvelles impressions de documents pour des inscrptions e-Facture : $(cat "$TMP_FILE" | wc -l) contribuables concernés."
	echo
	echo "NO_CTB;NOMBRE_DOCUMENTS_IMPRIMES"
	cat "$TMP_FILE"
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

function extract_date() {
	local FILE_NAME="$1"
	local DATE_IN_NAME=$(basename "$FILE_NAME" | sed -e 's/^.*\([0-9]\{4\}\)-\([0-9]\{2\}\)-\([0-9]\{2\}\).*$/\3.\2.\1/')
	if [[ "$DATE_IN_NAME" =~ [0-9]{2}.[0-9]{2}.[0-9]{4} ]]; then
		echo "$DATE_IN_NAME"
	else
		# date du jour...
		date +%d.%m.%Y
	fi
}


if [ -s "$TMP_FILE" ]; then
	if [ $# -ge 1 ]; then

		DATE_REF=$(extract_date "$SRC_FILE")
		ENVIRONMENT=$(basename "$SRC_FILE" | sed -e 's/.*\([A-Z]\{2\}\)01.*$/\1/')

		mutt -s "Impression de documents d'inscription e-Facture du $DATE_REF sur l'environnement $ENVIRONMENT" -- "$@" <<- EOF

			Bonjour !

			Ceci est un message automatique. En date du $DATE_REF, sur l'environnement Unireg $ENVIRONMENT, des demandes
			d'inscription e-facture ont été reçues, qui ont donné lieu à des envois de courriers.

			Ci-jointe une extraction des contribuables concernés ($(cat "$TMP_FILE" | wc -l)) :

			NO_CTB;NOMBRE_DOCUMENTS_IMPRIMES
			$(cat "$TMP_FILE")

			Cordialement,
			Votre registre fiscal.

		EOF

	else
		body | encode 
	fi


fi

rm -rf "$TMP_FILE"
