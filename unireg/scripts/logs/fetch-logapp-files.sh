#! /bin/bash -
# Récupération des fichiers de l'indexeur depuis le serveur logapp

ENVIRONMENT=$1
ROOT_DIR=$2
DEST_DIR=$3
if [ -z "$ENVIRONMENT" ]; then
        echo "Syntaxe : $(basename "$0") <env> <root> <dest> avec <env> l'un de PR, VA, PP, TE, <root> le répertoire (relatif à logapp/unireg/<env>) et <dest> le répertoire local de destination" >&2
        exit 1
elif [[ ! "$ENVIRONMENT" =~ ^(PR|VA|PP|TE)$ ]]; then
        echo "Pour l'environnement, seuls PR, VA, PP et TE sont acceptés (trouvé : '$ENVIRONMENT')" >&2
        exit 1
elif [ -z "$DEST_DIR" ]; then
	echo "Le répertoire de destination doit être donné en paramètre" >&2
	exit 1
elif [ -e "$DEST_DIR" ]; then
	echo "Le répertoire de destination existe déjà. Abandon." >&2
	exit 1
fi

# on rend maintenant le nom du répertoire de destination absolu
DEST_DIR=$(cd $(dirname "$DEST_DIR") && pwd)/$(basename "$DEST_DIR")

# cleanup avant nouvelle récupération
mkdir "$DEST_DIR"

MACHINE=logapp.etat-de-vaud.ch
URL="https://$MACHINE/unireg/$ENVIRONMENT/$ROOT_DIR/"
wget --no-proxy --no-check-certificate "$URL" -O - | grep "<a href=" | grep -v "Parent Directory" | while IFS="<>" read DUMMY1 HREF DUMMY2; do
	FILENAME=$(echo "$HREF" | sed -e 's/^.*href=.\(.*\).$/\1/')
	(cd "$DEST_DIR" && wget --no-proxy --no-check-certificate "$URL$FILENAME")
done
