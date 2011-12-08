#! /bin/bash -
# Sorte d'équivalent de la commande grep (en très limité quand-même !) qui fonctionne
# à la fois sur les fichiers compressés en lzma et les fichiers non-compressés
#
# Les variables d'environnement suivantes sont utilisées :
#	- AFTER : sera transcrit par le paramètre -A de grep
#	- BEFORE : sera transcrit par le paramètre -B de grep

REGEXP="$1"
if [ -z "$REGEXP" ]; then
	echo "Syntaxe : $(basename "$0") regexp file [file...]" >&2
	exit 1
fi

GREP_OPTS=""
if [ -n "$AFTER" ]; then
	GREP_OPTS="$GREP_OPTS -A $AFTER"
fi
if [ -n "$BEFORE" ]; then
	GREP_OPTS="$GREP_OPTS -B $BEFORE"
fi

function dumpfile() {
	FILE_TO_DUMP="$1"
	if [[ "$FILE_TO_DUMP" =~ \.lzma$ ]]; then
		lzcat "$FILE_TO_DUMP"
	else
		cat "$FILE_TO_DUMP"
	fi
}

shift
for FILE in "$@"; do
	dumpfile "$FILE" | grep $GREP_OPTS "$REGEXP" | while read LINE; do
		echo "$FILE:$LINE"
	done
done
