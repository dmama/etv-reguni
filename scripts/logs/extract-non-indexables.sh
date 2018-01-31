#! /bin/bash -

TODAY=$(date +%Y%m%d)

ENVIRONMENT=$1
DAY=$2
if [ -z "$ENVIRONMENT" -o -z "$DAY" ]; then
        echo "Syntaxe : $(basename "$0") <env> AAAAMMJJ toto@vd.ch [titi@vd.ch ...] avec <env> l'un de PR, VA, PP, TE" >&2
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

shift 2         # maintenant, les adresses mail sont dans "$@"

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

cat_file "$LOGFILE" | grep "\bMass-[0-9]\b" | grep "indexer le[s]\? tiers" | sed -e 's/^.*tiers n.//' | sed -e '/^[^0-9]/ s/[^0-9 ]\+//g' | sed -e '/:/! s/ /\n/g' | sort -un | while read LINE; do
	if [[ "$LINE" =~ ^[0-9]+$ ]]; then
		EXPL=$(cat_file "$LOGFILE" | grep -h "^[[:blank:]]\+$LINE" | sort -u | sed -e 's/^[[:blank:]]\+//')
		if [ -z "$EXPL" ]; then
			echo "$LINE"
		else
			echo "$EXPL"
		fi
	else
		echo "$LINE"
	fi
done | sed -e '/ServiceCivilException/ s/[a-zA-Z.]\+ServiceCivilException: //' > "$TMPFILE"

if [ -s "$TMPFILE" ]; then

	if [ $# -ge 1 ]; then

		NB_TIERS=$(cat "$TMPFILE" | wc -l)
		mutt -s "Tiers non-indexables de l'environnement Unireg $ENVIRONMENT en date du $FORMATTED_DAY" -- "$@" <<-EOF

			Bonjour !

			Ceci est un message automatique. En date du $FORMATTED_DAY, sur l'environnement Unireg $ENVIRONMENT, $NB_TIERS tiers a/ont été identifié(s) comme non-indexable(s) :

			$(cat "$TMPFILE")

			Cordialement,
			Votre registre fiscal.

		EOF

	else
		cat "$TMPFILE"
	fi

fi

rm -f "$TMPFILE"
