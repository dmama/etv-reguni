#! /bin/bash -
# Tentative de détection des "rafales" du CEDI dans le quittancement des déclarations d'impôt
# (rafale = tentative de quittancement, sur le ou les mêmes contribuables, toutes les 50-100 ms, pendant des heures...)

FILE="$1"
if [ -z "$FILE" ]; then
	echo "Syntaxe : $(basename "$0") ws-access.log toto@vd.ch [titi@vd.ch ...]" >&2
	exit 1
elif [ ! -r "$FILE" ]; then
	echo "Le fichier '$FILE' n'est pas accessible en lecture!" >&2
	exit 1
fi

# seuls les adresses mails sont maintenant dans "$@"
shift

function read_file() {
	if [[ "$1" =~ \.lzma$ ]]; then
		lzcat "$1"
	else
		cat "$1"
	fi
}

function filter_timestamp() {
	SEUIL="$1"
	sed -e 's/^.*\[\([0-9]\{4\}\)-\([0-9]\{2\}\)-\([0-9]\{2\}\) \([0-9]\{2\}\):\([0-9]\{2\}\):\([0-9]\{2\}\)[^]]*\]/\1\2\3\4\5\6/' | awk "\$1 > $SEUIL { print \$0; }"
}

# on ne s'intéresse qu'au 5 dernières minutes
PERIODE_MINUTES=5
TS_SEUIL=$(date +"%Y%m%d%H%M%S" --date "$PERIODE_MINUTES minutes ago")

# Au dela de cinquante appels pour le quittancement de la même DI, il y a un problème, non ?
ANALYSIS=$(read_file "$FILE" | grep "\[cedi\]" | grep "QuittancerDeclaration" | filter_timestamp $TS_SEUIL | sed -e 's/QuittanceDeclarationDemande/\n/g' | grep "DeclarationImpotOrdinaireKey" | sed -e 's/[^0-9 ]\+//g' | awk '{ print $1 "-" $2 "-" $3; }' | sort | uniq -c | awk '$1 > 49 { print $0; }')

# si la variable est vide, il n'y a plus rien à faire
MSG=$(if [ -n "$ANALYSIS" ]; then

	echo "Ceci est un message automatique..."
	echo
	echo "Il apparait que certaines DI ont été quittancées plus de 50 fois au cours de ces $PERIODE_MINUTES dernières minutes :"
	echo
	echo "$ANALYSIS" | while read COUNT ID; do
		echo "- DI $ID, quittancée $COUNT fois"
	done
	echo
	echo "Votre registre fiscal."

fi)

if [ -n "$MSG" ]; then
	if [ -z "$1" ]; then
		echo "$MSG"
	else
		echo "$MSG" | mutt -s "Alerte de quittancement de DI" -- "$@"
	fi
fi
