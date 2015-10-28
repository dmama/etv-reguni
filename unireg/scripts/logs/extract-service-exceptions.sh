#! /bin/bash -

TODAY=$(date +%Y%m%d)

ENVIRONMENT=$1
DAY=$2
SERVICE=$3
if [ -z "$ENVIRONMENT" -o -z "$DAY" -o -z "$SERVICE" ]; then
        echo "Syntaxe : $(basename "$0") <env> AAAAMMJJ ServiceName toto@vd.ch [titi@vd.ch ...] avec <env> l'un de PR, VA, PP, TE" >&2
        echo "Services connus :" >&2
        echo -e "\t- EvenementExterneListenerImpl" >&2
        echo -e "\t- PartyRequestListener" >&2
        echo -e "\t- EvenementCivilEchListener" >&2
        echo -e "\t- ..." >&2
        exit 1
elif [[ ! "$ENVIRONMENT" =~ ^(PR|VA|PP|TE)$ ]]; then
        echo "Pour l'environnement, seuls PR, VA, PP et TE sont acceptés (trouvé : '$ENVIRONMENT')" >&2
        exit 1
elif [[ ! "$DAY" =~ ^[0-9]{8}$ ]]; then
        echo "La date des logs à analyser doit être composée de 8 chiffres, sous la forme AAAAMMJJ (trouvé : '$DAY')" >&2
        exit 1
elif [ "$DAY" -lt 20090713 -o "$DAY" -gt "$TODAY" ]; then
        echo "La date demandée semble farfelue (trop vieille ou dans le futur) : $DAY ?" | sed -e 's/\([0-9]\{4\}\)\([0-9]\{2\}\)\([0-9]\{2\}\)/\3.\2.\1/' >&2
        exit 1
fi

# on se place dans le bon répertoire
cd "$(dirname "$0")/$ENVIRONMENT/unireg-web"

shift 3		# maintenant, les adresses mail sont dans "$@"

FORMATTED_DAY=$(echo "$DAY" | sed -e 's/\([0-9]\{4\}\)\([0-9]\{2\}\)\([0-9]\{2\}\)/\1-\2-\3/')
if [ "$DAY" -eq "$TODAY" ]; then
        LOGFILE="./unireg-web-cat_unireg${ENVIRONMENT}01.log"
else
        LOGFILE=$(find . -name "unireg-web-cat_unireg${ENVIRONMENT}01.log.$FORMATTED_DAY*")

        if [ -z "$LOGFILE" ]; then
                echo "Pas de fichier unireg-web trouvé pour la date du $FORMATTED_DAY" >&2
                exit 1
        fi
fi

function cat_file {
	if [[ "$1" =~ \.lzma$ ]] || [[ "$1" =~ \.xz$ ]]; then
		xzcat "$1"
	else
		cat "$1"
	fi
}

TMPFILE=$(mktemp)

cat_file "$LOGFILE" | grep "^[^[:blank:]].*\b$SERVICE\b.*Exception" | awk '{ print $5; }' | sed -e 's/^.\(.*\).$/\1/' | while read THREAD; do
	cat_file "$LOGFILE" | grep -n "^[^[:blank:]].*\b$THREAD\b.*\b$SERVICE\b" | grep "Exception" -B 1
done | grep -v "^--" | sed 's/:/ /' | while read LINE_NB D1 D2 DATE HOUR D3 D4 D5 PAYLOAD; do
	echo "${LINE_NB} ${DATE} ${HOUR} ${PAYLOAD}"
done | sed -e '/Exception/! s/^[0-9]\+[[:blank:]]\+//; /Exception/ s/\(^[0-9]\+\)[^0-9].*$/\1/' | while read LINE; do
	if [[ "$LINE" =~ ^[0-9]+$ ]]; then
		cat_file "$LOGFILE" | sed -e "$LINE,/^\[/! D" | sed -e '$ D' | sed -e '1 s/^.*| //' | uniq
	else
		read DATE HOUR REST < <(echo "$LINE")
		echo
		echo
		echo "---------------------------------------"
		echo -n "| Erreur à "
		echo -n "$HOUR le $DATE" | sed -e 's/[^A-Za-zà 0-9:.-]\+//g' | sed -e 's/\([0-9]\{4\}\)-\([0-9]\{2\}\)-\([0-9]\{2\}\)/\3.\2.\1/'
		echo " |"
		echo "---------------------------------------"
		echo "$LINE"
	fi
done | sed -e '1,2 D' | sed -e 's/^-$//' > "$TMPFILE"

if [ -s "$TMPFILE" ]; then

	HEADERS=$(grep "^\[" "$TMPFILE" | sed -e 's/^.*\]//' | sort | uniq -c | while read COUNT PAYLOAD; do echo "- $COUNT erreur(s) avec '$PAYLOAD'"; done)

	if [ $# -ge 1 ]; then

		mutt -s "Erreurs dans le service Unireg $SERVICE sur l'environnement $ENVIRONMENT en date du $FORMATTED_DAY" -- "$@" <<-EOF

			Bonjour !

			Ceci est un message automatique. En date du $FORMATTED_DAY, sur l'environnement Unireg $ENVIRONMENT, des erreurs sur le service $SERVICE ont été constatées.

			$(echo "$HEADERS")

			Ci-joints les extraits des logs :

			$(cat $TMPFILE)

			Cordialement,
			Votre registre fiscal.

		EOF


	else
		echo "------------------------------"
		echo "| Résumé                     |"
		echo "------------------------------"
		echo
		echo "$HEADERS"
		echo
		cat "$TMPFILE"
	fi

fi

rm -f "$TMPFILE"
