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
elif [ "$DAY" -lt 20141001 -o "$DAY" -gt "$TODAY" ]; then
        echo "La date demandée semble farfelue (trop vieille ou dans le futur) : $DAY ?" | sed -e 's/\([0-9]\{4\}\)\([0-9]\{2\}\)\([0-9]\{2\}\)/\3.\2.\1/' >&2
        exit 1
fi

# on se place dans le bon répertoire
cd "$(dirname "$0")/$ENVIRONMENT/unireg-web"

# after that, the mail addresses are in $1, $2...
shift 2

APP_LOG_FILE=
JMS_LOG_FILE=
FORMATTED_DAY=$(echo "$DAY" | sed -e 's/\([0-9]\{4\}\)\([0-9]\{2\}\)\([0-9]\{2\}\)/\1-\2-\3/')
if [ "$DAY" -eq "$TODAY" ]; then
        APP_LOG_FILE="./unireg-web-cat_unireg${ENVIRONMENT}01.log"
        JMS_LOG_FILE="./web-jms-access-cat_unireg${ENVIRONMENT}01.log"
else
        APP_LOG_FILE=$(find . -name "unireg-web-cat_unireg${ENVIRONMENT}01.log.$FORMATTED_DAY*")
        JMS_LOG_FILE=$(find . -name "web-jms-access-cat_unireg${ENVIRONMENT}01.log.$FORMATTED_DAY*")

        if [ -z "$APP_LOG_FILE" ]; then
                echo "Pas de fichier unireg-web trouvé pour la date du $FORMATTED_DAY" >&2
                exit 1
        elif [ -z "$JMS_LOG_FILE" ]; then
                echo "Pas de fichier web-jms-access trouvé pour la date du $FORMATTED_DAY" >&2
                exit 1
        fi
fi

if [ ! -r "$APP_LOG_FILE" ]; then
        echo "Le fichier $APP_LOG_FILE n'est pas accessible en lecture" >&2
        exit 1
elif [ ! -r "$JMS_LOG_FILE" ]; then
        echo "Le fichier $JMS_LOG_FILE n'est pas accessible en lecture" >&2
        exit 1
fi

function cat_file() {
        FILE=$1
        if [[ "$FILE" =~ \.lzma$ ]] || [[ "$FILE" =~ \.xz$ ]]; then
                xzcat "$FILE"
        else
                cat "$FILE"
        fi
}

function filter_log() {
	RELEVANT=YES
	while read LINE; do
		if [[ "$LINE" =~ vtReqDes ]]; then
			RELEVANT=YES
		fi

		# cette ligne est la dernière concernant un traitement
		if [[ "$LINE" =~ vtReqDes.*terminé.dans.l.état ]]; then
			echo "$LINE"
			echo "---"
			RELEVANT=NO
		elif [ "$RELEVANT" == "YES" ]; then
			echo "$LINE"
		fi
	done
}

function reorder-error-messages() {
	BUFFER=""
	while read LINE; do
		if [[ "$LINE" =~ ^-[[:blank:]].*$ ]] || [[ "$LINE" =~ Exception ]]; then
			if [[ ! "$LINE" =~ ^- ]]; then
				LINE="- $LINE"
			fi
			if [ -z "$BUFFER" ]; then
				BUFFER="$LINE"
			else
				BUFFER=$(echo -e "$BUFFER\n$LINE")
			fi
		else
			echo "$LINE"
			echo "$BUFFER"
			echo
			BUFFER=""
		fi
	done
}

TMP_DIR=$(mktemp -d)
APP_LOG="$TMP_DIR/unireg-web.log"
STATS_LOG="$TMP_DIR/stats"
DETAIL_ERRORS="$TMP_DIR/errors"
JMS_LOG="$TMP_DIR/web-jms-access.log"

# dans le fichier jms, on recherche les messages entrants
cat_file "$JMS_LOG_FILE" | grep "unireg\.reqdes" > "$JMS_LOG"

# dans le fichier app, on recherche les traitements effectués (je prends les 10 lignes suivantes pour prendre aussi les messages d'erreurs...)
cat_file "$APP_LOG_FILE" | grep "vtReqDes" -A 10 | filter_log > "$APP_LOG"

# calcul de quelques statistiques
cat "$APP_LOG" | grep "terminé dans l.état" | sed -e 's/^.* \([A-Z_]\+\)$/\1/' | sort | uniq -c > "$STATS_LOG"

# affichage des message d'erreur
cat "$APP_LOG" | grep "\(^- \|EN_ERREUR\|^[^ ]\+Exception\)" | grep -v "ReqDesWrapping" | uniq | sed -e '/vtReqDes/ s/^.*[0-9]\{4\}-[0-9]\{2\}-[0-9]\{2\} \([0-9]\{2\}:[0-9]\{2\}:[0-9]\{2\}\.[0-9]\+\)[^0-9].*traitement \([0-9]\+\) .*$/Unité de traitement \2 à \1 :/' | reorder-error-messages > "$DETAIL_ERRORS"

# constitution du message en sortie
if [ -s "$STATS_LOG" ]; then

	if [ $# -ge 1 ]; then

		mutt -s "Traitements ReqDes dans l'application Unireg sur l'environnement $ENVIRONMENT en date du $FORMATTED_DAY" -- "$@" <<-EOF
			Bonjour !

			Ceci est un message automatique. En date du $FORMATTED_DAY, sur l'environnement Unireg $ENVIRONMENT, des activités ReqDes ont été observées.

			Ci-jointes quelques informations extraites des logs applicatifs.


			1. Messages JMS ReqDes reçus au cours de la journée ($(cat "$JMS_LOG" | wc -l)) :

			$(cat "$JMS_LOG")


			2. Répartition par état final des unités de traitement ReqDes traitées au cours de la journée :

			$(cat "$STATS_LOG")


			3. Détail des erreurs de traitement rencontrées :

			$(cat "$DETAIL_ERRORS")


			Cordialement,
			Votre registre fiscal.

		EOF

	else

		echo "Messages JMS ReqDes reçus le $FORMATTED_DAY sur l'environnement $ENVIRONMENT :"
		echo
		cat "$JMS_LOG"
		echo
		echo
		echo "Répartitions des états finaux des unités de traitement :"
		echo
		cat "$STATS_LOG"
		echo
		echo
		echo "Détail des erreurs :"
		echo
		cat "$DETAIL_ERRORS"

	fi

fi

rm -rf "$TMP_DIR"

