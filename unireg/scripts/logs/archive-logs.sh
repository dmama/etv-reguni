#! /bin/bash -
# archive les fichiers de log unireg dans un répertoire mensuel
# (ne pas archiver de fichier de moins de 20 jours car ils seraient
# récupérés le lendemain depuis le serveur)

ENVIRONMENT=$1
OLD_NEW=$2
if [ -z "$ENVIRONMENT" ]; then
	echo "Syntaxe : $(basename "$0") <env> avec <env> l'un de PR, PO, VA, PP, FO" >&2
	exit 1
elif [[ ! "$ENVIRONMENT" =~ ^(PR|PO|VA|PP|FO)$ ]]; then
	echo "Pour l'environnement, seuls PR, PO, VA, PP et FO sont acceptés (trouvé : '$ENVIRONMENT')" >&2
	exit 1
elif [ -n "$OLD_NEW" -a "$OLD_NEW" != "old" ]; then
        echo "Syntaxe : $(basename "$0") <env> [old] avec <env> l'un de PR, PO, VA, PP, FO" >&2
        exit 1
fi

# les fichiers sont téléchargés dans un sous-répertoire spécifique à l'environnement voulu
cd "$(dirname "$0")/$ENVIRONMENT"

if [ -n "$OLD_NEW" ]; then
	DEPTH=1
else
	DEPTH=2
fi

find . -maxdepth $DEPTH -type f -name "*.log.*" -mtime +20 | while read FILE; do

	LOGDATE=$(echo "$FILE" | sed -e 's/[^0-9\.-]//g' | awk -F"." '{ print $4; }')
	LOGMONTH_DIR=$(dirname "$FILE")
	LOGMONTH="$LOGMONTH_DIR/$(echo "$LOGDATE" | cut -d"-" -f1,2)"
	if [ ! -d "$LOGMONTH" ]; then
		mkdir "$LOGMONTH"
	fi
	echo "Moving file $FILE to archive directory $LOGMONTH"
	mv "$FILE" "$LOGMONTH"

done
