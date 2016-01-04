#! /bin/bash -

TMP_DIR=$(mktemp -d)
TMP_FILE="$TMP_DIR/file"
TMP_DEMANDES="$TMP_DIR/demandes"
touch "$TMP_DEMANDES"

xzgrep "EFactureEventHandlerImpl" "$@" | grep "\binscription e-Facture" -A 2 | grep -v "en cours" | sed -e '/Reçu/ N;s/\n/ /' | grep -v "^--" | while read LINE; do

	if [[ "$LINE" =~ ch\.vd\. ]]; then
		# Exception....
		echo "$LINE" | sed -e 's/^.*\b\([0-9]\+\). du contribuable \([0-9./]\+\) au \([0-9.]\+\) .*$/\3;\2;EXCEPTION;\1/' 
	else
		echo "$LINE" | sed -e 's/^.*\b\([0-9]\+\). du contribuable \([0-9./]\+\) au \([0-9.]\+\) .* \([A-Z_]\+\)$/\3;\2;\4;\1/' 
	fi | while IFS=";" read DATE CTB_AVS TRAITEMENT NO_DEMANDE; do
		grep -x "$NO_DEMANDE" "$TMP_DEMANDES" 2>/dev/null 1>&2
		if [ $? == 0 ]; then
			TRAITEMENT="$TRAITEMENT (RELANCE)"
		fi
		echo "$NO_DEMANDE" >> "$TMP_DEMANDES"
		echo "$DATE;$CTB_AVS;$TRAITEMENT;$NO_DEMANDE"

	done | sed -e 's/\//;/' -e 's/^\([0-9]\{2\}\)\.\([0-9]\{2\}\)\.\([0-9]\{4\}\)/\3\2\1/' -e 's/\.//g'
done > "$TMP_FILE"

function body() {
	echo "Nouvelles inscriptions e-Facture : $(grep -v "RELANCE" "$TMP_FILE" -c)."
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

rm -rf "$TMP_DIR"
