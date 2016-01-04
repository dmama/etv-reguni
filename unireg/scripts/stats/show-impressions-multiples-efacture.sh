#! /bin/bash -

TMP_FILE=$(mktemp)

./show-inscriptions-efacture.sh "$@" | grep "EN_ATTENTE" | awk -F";" '{ print $2; }' | sort | uniq -c | sort -n | awk '{ print $2 ";" $1; }' > "$TMP_FILE"

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


if [ -s "$TMP_FILE" ]; then
	body | encode 
fi

rm -rf "$TMP_FILE"
