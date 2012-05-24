#! /bin/bash -

SERVICE="PartyRequestListener"

function cat_file() {
	for FILE in "$@"; do
		if [[ "$FILE" =~ \.lzma$ ]]; then
			lzcat "$FILE"
		else
			cat "$FILE"
		fi
	done
}

for FILE in "$@"; do

	cat_file "$FILE" | grep -n "$SERVICE" | grep "Exception" -B 1 | grep -v "^--$" | (while read INCOMING_MSG_LINE; do
		read ERROR_HEADER_LINE
		IFS=":" read LINE_NB ERROR_MSG < <(echo "$ERROR_HEADER_LINE")
		STACK=$(cat_file "$FILE" | sed -e "1,$LINE_NB D" -e '/^\[unireg\]/,$ D')
	
		echo "-- Fichier $FILE:"
		echo "--"
		IFS=":" read DUMMY_LINE_NB MSG < <(echo "$INCOMING_MSG_LINE")
		echo "$MSG"
		echo "$ERROR_MSG"
		echo "$STACK"
		echo
		echo
	done)

done
