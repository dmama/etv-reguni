#! /bin/bash -
# Récupération des fichiers de l'indexeur depuis le serveur logapp
# A utiliser pour lancer tard dans la nuit ou pendant que l'application ne tourne pas, afin d'avoir un état cohérent

ENVIRONMENT=$1
INDEX=$2
DEST_FILE=$3
if [ -z "$ENVIRONMENT" ]; then
        echo "Syntaxe : $(basename "$0") <env> <tiers|message-identification> <dest-file>.tar.xz avec <env> l'un de PR, VA, PP, TE" >&2
        exit 1
elif [[ ! "$ENVIRONMENT" =~ ^(PR|VA|PP|TE)$ ]]; then
        echo "Pour l'environnement, seuls PR, VA, PP et TE sont acceptés (trouvé : '$ENVIRONMENT')" >&2
        exit 1
elif [ -z "$INDEX" ]; then
	echo "L'index souhaité doit être donné en paramètre" >&2
	exit 1
elif [[ ! "$INDEX" =~ ^(tiers|message-identification)$ ]]; then
	echo "Seuls les indexes 'tiers' et 'message-identification' sont reconnus (trouvé : '$INDEX')" >&2
	exit 1
elif [ -z "$DEST_FILE" ]; then
	echo "Le fichier de destination doit être donné en paramètre" >&2
	exit 1
elif [[ ! "$DEST_FILE" =~ \.tar\.xz$ ]]; then
	echo "Le nom du fichier de destination doit se terminer en .tar.xz" >&2
	exit 1
elif [ -e "$DEST_FILE" ]; then
	echo "Le fichier de destination existe déjà. Abandon." >&2
	exit 1
fi

# on rend maintenant le nom du fichier de destination absolu
DEST_FILE=$(cd $(dirname "$DEST_FILE") && pwd)/$(basename "$DEST_FILE")

# cleanup avant nouvelle récupération
LUCENE_DIR="lucene/$INDEX"
TMP_DIR=$(mktemp -d)
mkdir "$TMP_DIR/$INDEX"

MACHINE=logapp.etat-de-vaud.ch
URL="https://$MACHINE/unireg/$ENVIRONMENT/unireg-web/$LUCENE_DIR/"
wget --no-proxy --no-check-certificate "$URL" -O - | grep "<a href=" | grep -v "Parent Directory" | while IFS="<>" read DUMMY1 HREF DUMMY2; do
	FILENAME=$(echo "$HREF" | sed -e 's/^.*href=.\(.*\).$/\1/')
	(cd "$TMP_DIR/$INDEX" && wget --no-proxy --no-check-certificate "$URL$FILENAME")
done

echo "Génération du fichier d'archive $DEST_FILE..." >&2
(cd "$TMP_DIR" && tar --create --use-compress-program /usr/bin/xz --verbose --file "$DEST_FILE" "$INDEX")
rm -rf "$TMP_DIR"
