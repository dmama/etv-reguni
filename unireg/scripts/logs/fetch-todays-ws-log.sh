#! /bin/bash -
# Récupère les fichiers de log du jour de l'application WS (en opposition à l'application WEB)

ENVIRONMENT=$1
if [ -z "$ENVIRONMENT" ]; then
        echo "Syntaxe : $(basename "$0") <env> [old] avec <env> l'un de PR, PO, VA, PP, FO" >&2
        exit 1
elif [[ ! "$ENVIRONMENT" =~ ^(PR|PO|VA|PP|FO)$ ]]; then
        echo "Pour l'environnement, seuls PR, PO, VA, PP et FO sont acceptés (trouvé : '$ENVIRONMENT')" >&2
        exit 1
fi

# les fichiers sont téléchargés dans un sous-répertoire spécifique à l'environnement voulu
cd "$(dirname "$0")/$ENVIRONMENT/unireg-ws"

# fichier de lock présent
LOCK_FILE=".incremental.lock"
if [ -e "$LOCK_FILE" ]; then
        echo "Fichier de lock déjà présent (un processus tourne encore ?)... Abandon." >&2
        exit 2
fi

touch "$LOCK_FILE"

MACHINE=logapp.etat-de-vaud.ch
TOMCAT_PART="-cat_unireg${ENVIRONMENT}01"
FILES="unireg-ws${TOMCAT_PART}.log ws-access${TOMCAT_PART}.log srv-access-ws${TOMCAT_PART}.log"

function fetch() {
	WGET_OPT=""
	FILE="$1"
	if [ -e "$FILE" ]; then
		DATE_FILE=$(ls -l --time-style=long-iso "$FILE" | awk '{ print $6; }')
		TODAY=$(date +"%Y-%m-%d")
		if [ "$DATE_FILE" != "$TODAY" ]; then
			rm -f "$FILE"
		else
			WGET_OPT="-c"
		fi
	fi

	wget --no-check-certificate $WGET_OPT https://$MACHINE/unireg/$ENVIRONMENT/unireg-ws/logs/$FILE
}

for LOG in $FILES; do
	fetch "$LOG"
done

rm "$LOCK_FILE"
