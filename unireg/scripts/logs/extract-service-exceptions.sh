#! /bin/bash -

SERVICE="$1"

if [ -z "$SERVICE" -o "$#" -lt 2 ]; then
	echo "Syntaxe: $(basename "$0") ServiceName weblogfile1 [weblogfile2 [...]]" >&2
	echo "Services connus :" >&2
	echo -e "\t- EvenementExterneListenerImpl" >&2
	echo -e "\t- PartyRequestListener" >&2
	echo -e "\t- ..." >&2
	exit 1
fi

shift

function cat_file {
	if [[ "$1" =~ \.lzma$ ]]; then
		lzcat "$1"
	else
		cat "$1"
	fi
}

for FILE in "$@"; do

	cat_file "$FILE" | grep -n "$SERVICE" | grep -B 1 "Exception" | grep -v "^--" | sed -e 's/:/ /' | while read LINE_NB D1 D2 DATE HOUR D3 D4 D4 PAYLOAD; do
		echo "${LINE_NB} ${DATE} ${HOUR} ${PAYLOAD}"
	done | sed -e '/Exception/! s/^[0-9]\+[[:blank:]]\+//; /Exception/ s/\(^[0-9]\+\)[^0-9].*$/\1/' | while read LINE; do
		if [[ "$LINE" =~ ^[0-9]+$ ]]; then
			cat_file "$FILE" | sed -e "$LINE,/^\[/! D" | sed -e '$ D' | sed -e '1 s/^.*| //'
		else
			read DATE HOUR REST < <(echo "$LINE")
			echo
			echo
			echo "---------------------------------------"
			echo -n "| Erreur à "
			echo -n "$HOUR le $DATE" | sed -e 's/[^A-Za-zà 0-9:.-]\+//g' | sed -e 's/\([0-9]\{4\}\)-\([0-9]\{2\}\)-\([0-9]\{2\}\)/\3.\2.\1/'
			echo " |"
			echo "---------------------------------------"
			echo "$LINE"
		fi
	done | sed -e '/^.\+$/,$ s/^$/-/' | sed -e '/^$/ D' | sed -e 's/^-$//'		# tout ça pour enlever les deux lignes vides du début du fichier...

done
