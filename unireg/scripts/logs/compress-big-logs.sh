#! /bin/bash -
# Compresse les logs qui font plus de 200M déjà en avance (= ne pas forcément attendre 10 jours)
# JDE, 14.07.2011 : on ne compresse pas les fichiers dans le répertoire IN (pour l'intégration) car ils sont obtenus par rsync
find $HOME/logs -name "*.log.????-??-??" -size +200M | grep -v "/IN/" | while read FILE; do
	BEFORE=$(stat --format "%s" "$FILE")
	lzma "$FILE"
	AFTER=$(stat --format "%s" "$FILE.lzma")
	echo "Compression of big log file $FILE done: $BEFORE -> $AFTER (bytes)"
done
