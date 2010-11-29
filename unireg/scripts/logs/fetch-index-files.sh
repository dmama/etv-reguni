#! /bin/bash -
# Récupération des fichiers de l'indexeur depuis le serveur logapp
# A utiliser pour lancer tard dans la nuit ou pendant que l'application ne tourne pas, afin d'avoir un état cohérent

ENVIRONMENT=$1
DEST_FILE=$2
if [ -z "$ENVIRONMENT" ]; then
        echo "Syntaxe : $(basename "$0") <env> <dest-file>.tar.lzma avec <env> l'un de PR, PO, VA, PP, FO" >&2
        exit 1
elif [[ ! "$ENVIRONMENT" =~ ^(PR|PO|VA|PP|FO)$ ]]; then
        echo "Pour l'environnement, seuls PR, PO, VA, PP et FO sont acceptés (trouvé : '$ENVIRONMENT')" >&2
        exit 1
elif [ -z "$DEST_FILE" ]; then
	echo "Le fichier de destination doit être donné en paramètre" >&2
	exit 1
elif [[ ! "$DEST_FILE" =~ \.tar\.lzma$ ]]; then
	echo "Le nom du fichier de destination doit se terminer en .tar.lzma" >&2
	exit 1
elif [ -e "$DEST_FILE" ]; then
	echo "Le fichier de destination existe déjà. Abandon." >&2
	exit 1
fi

# les fichiers sont téléchargés dans un sous-répertoire spécifique à l'environnement voulu
cd "$(dirname "$0")/$ENVIRONMENT"

# cleanup avant nouvelle récupération
LUCENE_DIR=lucene
TMP_DIR=$(mktemp -d)

MACHINE=logapp.etat-de-vaud.ch
URL="https://$MACHINE/unireg/$ENVIRONMENT/unireg-web/$LUCENE_DIR/"
wget --no-check-certificate "$URL" -O - | grep "<a href=" | grep -v "Parent Directory" | while IFS="<>" read DUMMY1 HREF DUMMY2; do
	FILENAME=$(echo "$HREF" | sed -e 's/^.*href=.\(.*\).$/\1/')
	(cd "$TMP_DIR" && wget --no-check-certificate "$URL$FILENAME")
done

echo "Génération du fichier d'archive $DEST_FILE..." >&2
(cd "$TMP_DIR" && tar --create --use-compress-program /usr/bin/lzma --verbose --file "$DEST_FILE" .)
rm -rf "$TMP_DIR"
