#! /bin/bash -
# Récupère les fichiers de log du jour de l'application WEB (par opposition à WS)
# Peut être utilisé dans un cron pour simuler un "tail" sur ces fichiers de log

ENVIRONMENT=$1
OLD_NEW=$2
if [ -z "$ENVIRONMENT" ]; then
        echo "Syntaxe : $(basename "$0") <env> [old] avec <env> l'un de PR, PO, VA, PP, FO" >&2
        exit 1
elif [[ ! "$ENVIRONMENT" =~ ^(PR|PO|VA|PP|FO)$ ]]; then
        echo "Pour l'environnement, seuls PR, PO, VA, PP et FO sont acceptés (trouvé : '$ENVIRONMENT')" >&2
        exit 1
elif [ -n "$OLD_NEW" -a "$OLD_NEW" != "old" ]; then
        echo "Syntaxe : $(basename "$0") <env> [old] avec <env> l'un de PR, PO, VA, PP, FO" >&2
        exit 1
fi

UNE_SEULE_WEB_APP=""
if [ -n "$OLD_NEW" ]; then
	UNE_SEULE_WEB_APP="Y"
fi

# les fichiers sont téléchargés dans un sous-répertoire spécifique à l'environnement voulu
if [ -n "$UNE_SEULE_WEB_APP" ]; then
	cd "$(dirname "$0")/$ENVIRONMENT"
else 
	cd "$(dirname "$0")/$ENVIRONMENT/unireg-web"
fi

# fichier de lock présent
LOCK_FILE=".incremental.lock"
if [ -e "$LOCK_FILE" ]; then
        echo "Fichier de lock déjà présent (un processus tourne encore ?)... Abandon." >&2
        exit 2
fi

touch "$LOCK_FILE"

MACHINE=logapp.etat-de-vaud.ch
TOMCAT_PART="-cat_unireg${ENVIRONMENT}01"
FILES="unireg-web${TOMCAT_PART}.log srv-access-web${TOMCAT_PART}.log"

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

	if [ -n "$UNE_SEULE_WEB_APP" ]; then
		wget --no-check-certificate $WGET_OPT https://$MACHINE/unireg/$ENVIRONMENT/logs/$FILE
	else
		wget --no-check-certificate $WGET_OPT https://$MACHINE/unireg/$ENVIRONMENT/unireg-web/logs/$FILE
	fi
}

for LOG in $FILES; do
        fetch "$LOG"
done

rm "$LOCK_FILE"
