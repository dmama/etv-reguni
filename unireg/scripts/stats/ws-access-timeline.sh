#! /bin/bash -
# A partir d'un fichier ws-access.log, génère une liste des temps de réponse (à une méthode du web-service donnée)
# répartis dans le temps (afin par exemple de mettre ça sur un graphe)

FILE="$1"
SRV=$2
if [ -z "$FILE" ]; then
	echo "Syntaxe : $(basename "$0") ws-access.log [nom-de-méthode]" >&2
	exit 1
elif [ ! -r "$FILE" -o ! -f "$FILE" ]; then
	echo "Fichier $FILE inaccessible" >&2
	exit 1
fi

SERVICES=$(awk '{ print $8; }' "$FILE" | sed -e 's/{.*$//' | sort | uniq)
if [ -z "$SRV" ]; then
	NB_SERVICES=$(echo "$SERVICES" | wc -l)
	if [ "$NB_SERVICES" -gt 1 ]; then
		IDX=1
		echo "$SERVICES" | while read LINE; do
			echo -e "\t$IDX:\t$LINE" >&2
			((++IDX))
		done
		CHOSEN=""
		while [[ ! "$CHOSEN" =~ ^[1-9][0-9]*$ || "$CHOSEN" -gt "$NB_SERVICES" ]]; do
			read -p "Quel est le service que l'on veut étudier ici? (1..$NB_SERVICES) : " CHOSEN;
		done
	
		SERVICE=$(echo "$SERVICES" | sed -e "$CHOSEN !D")
	else
		SERVICE="$SERVICES"
	fi
else
	SERVICE=$(echo "$SERVICES" | grep "^$SRV\$")
	if [ -z "$SERVICE" ]; then
		echo "Service '$SRV' inconnu, les services existants sont $(echo "$SERVICES" | xargs | sed -e 's/ /,/g')" >&2
		exit 1
	fi
fi
echo "Service choisi : $SERVICE" >&2

function remove-leading-zeroes() {
	sed -e 's/\b0\+\([1-9]\)/\1/g'
}

grep "\b$SERVICE{" "$FILE" | awk '{ print $4 " " $6; }' | sed -e 's/[^0-9\.: ]//g' | while IFS=":. " read HH MM SS SSS NB; do
	read HHC MMC SSC < <(echo "$HH $MM $SS" | remove-leading-zeroes)
	SSSC="$SSS"
	while [ ${#SSSC} -lt 3 ]; do
		SSSC="${SSSC}0"
	done
	SSSC=$(echo "$SSSC" | remove-leading-zeroes)
	((TIME=SSSC + SSC * 1000 + MMC * 60000 + HHC * 3600000))
	echo "$HH:$MM:$SS.$SSS;$TIME;$NB"
done
