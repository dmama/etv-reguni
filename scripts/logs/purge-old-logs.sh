#! /bin/bash -

ENVIRONMENT="$1"
LIMIT="$2"
if [ -z "$ENVIRONMENT" ]; then
        echo "Syntaxe : $(basename "$0") <env> <days> avec <env> l'un de PR, VA, PP, TE, IN et <days> l'âge maximal de conservation des logs dans cet environnement" >&2
        exit 1
elif [[ ! "$ENVIRONMENT" =~ ^(PR|VA|PP|IN|TE)$ ]]; then
        echo "Pour l'environnement, seuls PR, VA, PP, TE et IN sont acceptés (trouvé : '$ENVIRONMENT')" >&2
        exit 1
elif [[ ! "$LIMIT" =~ ^0*[1-9][0-9]*$ ]]; then
	echo "L'âge maximal de conservation des logs doit être un entier positif (trouvé : '$LIMIT')" >&2
	exit 1
fi

find -H ~/logs/$ENVIRONMENT -name "*.log*" -type f -mtime +$LIMIT | while read FILE; do if [ -e "$FILE" ]; then rm -f "$FILE"; fi; done
find -H ~/logs/$ENVIRONMENT -type d -empty | while read DIR; do if [ -d "$DIR" ]; then rmdir "$DIR"; fi; done
find -H ~/logs/$ENVIRONMENT -mindepth 2 -type d -mtime +$(($LIMIT + 31)) ! -path "*/repository/*" | while read DIR; do if [ -d "$DIR" ]; then rm -rf "$DIR"; fi; done
