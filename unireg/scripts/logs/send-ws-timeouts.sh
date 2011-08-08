#! /bin/bash -

TODAY=$(date +%Y%m%d)

ENVIRONMENT=$1
DAY=$2
if [ -z "$ENVIRONMENT" -o -z "$DAY" ]; then
        echo "Syntaxe : $(basename "$0") <env> AAAAMMJJ toto@vd.ch [titi@vd.ch ...] avec <env> l'un de PR, PO, VA, PP, FO" >&2
        exit 1
elif [[ ! "$ENVIRONMENT" =~ ^(PR|PO|VA|PP|FO)$ ]]; then
        echo "Pour l'environnement, seuls PR, PO, VA, PP et FO sont acceptés (trouvé : '$ENVIRONMENT')" >&2
        exit 1
elif [[ ! "$DAY" =~ ^[0-9]{8}$ ]]; then
	echo "La date des logs à analyser doit être composée de 8 chiffres, sous la forme AAAAMMJJ (trouvé : '$DAY')" >&2
	exit 1
elif [ "$DAY" -lt 20090713 -o "$DAY" -gt "$TODAY" ]; then
	echo "La date demandée semble farfelue (trop vieille ou dans le futur) : $DAY ?" | sed -e 's/\([0-9]\{4\}\)\([0-9]\{2\}\)\([0-9]\{2\}\)/\3.\2.\1/' >&2
	exit 1
fi

# on se place dans le bon répertoire
cd "$(dirname "$0")/$ENVIRONMENT/unireg-ws"

# after that, the mail addresses are in $1, $2...
shift 2

WS_ACCESS_FILE=
SRV_ACCESS_FILE=
FORMATTED_DAY=$(echo "$DAY" | sed -e 's/\([0-9]\{4\}\)\([0-9]\{2\}\)\([0-9]\{2\}\)/\1-\2-\3/')
if [ "$DAY" -eq "$TODAY" ]; then
	WS_ACCESS_FILE="./ws-access-cat_unireg${ENVIRONMENT}01.log"
	SRV_ACCESS_FILE="./srv-access-ws-cat_unireg${ENVIRONMENT}01.log"
else
	WS_ACCESS_FILE=$(find . -name "ws-access-cat_unireg${ENVIRONMENT}01.log.$FORMATTED_DAY*")
	SRV_ACCESS_FILE=$(find . -name "srv-access-ws-cat_unireg${ENVIRONMENT}01.log.$FORMATTED_DAY*")
fi

function cat_file() {
	FILE=$1
	if [[ "$FILE" =~ \.lzma$ ]]; then
		lzcat "$FILE"
	else
		cat "$FILE"
	fi
}

TMP_DIR=$(mktemp -d)
WS_LONG="$TMP_DIR/ws-access.log"
SRV_LONG="$TMP_DIR/srv-access.log"

# sur le fichier ws-access, on cherche les temps de réponse supérieurs à la minute
cat_file "$WS_ACCESS_FILE" | grep "(\([6-9][0-9]\{4\}\|[1-9][0-9]\{5,\}\) ms)" > "$WS_LONG"

# sur le fichier srv-access, on cherche les temps de réponse supérieurs à 30 secondes
cat_file "$SRV_ACCESS_FILE" | grep "(\([3-9][0-9]\{4\}\|[1-9][0-9]\{5,\}\) ms)" > "$SRV_LONG"

if [ -s "$WS_LONG" -o -s "$SRV_LONG" ]; then

	if [ $# -ge 1 ]; then

		mutt -s "Temps de réponse lents dans l'application Unireg sur l'environnement $ENVIRONMENT en date du $FORMATTED_DAY" -- "$@" <<-EOF
			Bonjour !
			
			Ceci est un message automatique. En date du $FORMATTED_DAY, sur l'environnement UNIREG $ENVIRONMENT, des temps de réponse importants ont été constatés.
			
			Ci-joints les extraits des fichiers de logs :

			1. Partie web-services unireg (temps supérieurs à la minute)

			$(cat "$WS_LONG")

			2. Partie services externes (temps supérieurs à la demi-minute)

			$(cat "$SRV_LONG")
			
			Cordialement,
			Votre registre fiscal.
		EOF

	else

		echo "Temps de réponse lents le $FORMATTED_DAY sur l'environnement $ENVIRONMENT :"
		echo
		echo "- Partie web-services :"
		cat "$WS_LONG"
		echo
		echo "- Partie services externes :"
		cat "$SRV_LONG"

	fi

fi

rm -rf "$TMP_DIR"
