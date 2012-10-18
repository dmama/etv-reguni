#! /bin/bash -

TMP_FILE=$(mktemp)

grep "EFactureEventHandlerImpl" "$@" | grep "\binscription e-Facture" -A 1 | sed -e '1~2 N;s/\n/ /' | sed -e 's/^.*\b\([0-9]\+\). du contribuable \([0-9./]\+\) au \([0-9.]\+\) .* \([A-Z_]\+\)$/\3\;\2\;\4;\1/' -e 's/\//;/' -e 's/^\([0-9]\{2\}\)\.\([0-9]\{2\}\)\.\([0-9]\{4\}\)/\3\2\1/' -e 's/\.//g' > "$TMP_FILE"

function body() {
	echo "Nouvelles inscriptions e-Facture : $(cat "$TMP_FILE" | wc -l)."
	echo
	echo "DATE;NO_CTB;NO_AVS;TRAITEMENT;NO_DEMANDE"
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

rm -f "$TMP_FILE"
