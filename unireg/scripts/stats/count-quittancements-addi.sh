#! /bin/bash -
# Compte le nombre de messages reçu pour quittancement ADDI depuis la date indiquée en paramètre sur l'environnement indiqué

ENVIRONMENT=$1
DEPUIS=$2
if [ -z "$ENVIRONMENT" -o -z "$DEPUIS" ]; then
        echo "Syntaxe : $(basename "$0") <env> <depuis-le> avec <env> l'un de PR, PO, VA, PP, FO et <depuis-le> au format YYYYMMDD" >&2
        exit 1
elif [[ ! "$ENVIRONMENT" =~ ^(PR|PO|VA|PP|FO)$ ]]; then
        echo "Pour l'environnement, seuls PR, PO, VA, PP et FO sont acceptés (trouvé : '$ENVIRONMENT')" >&2
        exit 1
elif [[ ! "$DEPUIS" =~ [0-9]{8} ]]; then
	echo "La date 'depuis-le' doit être au format YYYYMMDD (trouvé : '$DEPUIS')" >&2
	exit 1
fi

shift
shift

DEPUIS_SEC=$(date -d "$DEPUIS" +%s)
NOW_SEC=$(date +%s)
DIFF_DAYS=$((($NOW_SEC - $DEPUIS_SEC) / 86400 + 1))
COUNT=$(find ~/logs/PR/unireg-web/ -name "unireg-web*" -daystart -mtime -$DIFF_DAYS -mtime +0 | xargs ~/logs/greplogs.sh "QuittancementDI{businessId=" | grep ADDI -c)

BASE_MSG="Depuis le $DEPUIS (aujourd'hui non-compris), Unireg a reçu, sur l'environnement $ENVIRONMENT, la somme de $COUNT quittancements de déclarations originaires d'ADDI"
if [ -z "$@" ]; then
	echo "$BASE_MSG"
else
	MSG=$(
		echo "Ceci est un message automatique."
		echo
		echo "$BASE_MSG"
		echo
		echo "Votre registre fiscal."
	)
	echo "$MSG" | mutt -s "Quittancements de déclarations ADDI -> UNIREG" -- "$@"
fi
