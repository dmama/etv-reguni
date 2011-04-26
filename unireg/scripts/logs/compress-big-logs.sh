#! /bin/bash -
# Compresse les logs qui font plus de 200M déjà en avance (= ne pas forcément attendre 10 jours)
find $HOME/logs -name "*.log.????-??-??" -size +200M | while read FILE; do
	BEFORE=$(stat --format "%s" "$FILE")
	lzma "$FILE"
	AFTER=$(stat --format "%s" "$FILE.lzma")
	echo "Compression of big log file $FILE done: $BEFORE -> $AFTER (bytes)"
done
