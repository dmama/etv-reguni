#! /bin/bash -
# Récupération des fichiers des rapports d'exécution des batch

ENVIRONMENT=$1
if [ -z "$ENVIRONMENT" ]; then
        echo "Syntaxe : $(basename "$0") <env> avec <env> l'un de PR, PO, VA, PP, FO" >&2
        exit 1
elif [[ ! "$ENVIRONMENT" =~ ^(PR|PO|VA|PP|FO)$ ]]; then
        echo "Pour l'environnement, seuls PR, PO, VA, PP et FO sont acceptés (trouvé : '$ENVIRONMENT')" >&2
        exit 1
fi

# les fichiers sont téléchargés dans un sous-répertoire spécifique à l'environnement voulu
cd "$(dirname "$0")/$ENVIRONMENT"

# la hierarchie commence avec un répertoire "repository"
REPOSITORY_DIR="repository"
if [ ! -e "$REPOSITORY_DIR" ]; then
	mkdir "$REPOSITORY_DIR"
elif [ ! -d "$REPOSITORY_DIR" ]; then
	echo "Le fichier '$(pwd)/$REPOSITORY_DIR' existe, mais ce n'est pas un répertoire" >&2
	exit 1
fi

# l'url du répertoire dont il faut récupérer les noms de fichiers doit être en premier paramètre
function fetch-filenames() {
	wget --no-check-certificate "$1" -O - 2> /dev/null | grep "<a href=" | grep -v "Parent Directory" | while IFS="<>" read DUMMY1 HREF DUMMY2; do
		echo "$HREF"
	done | sed -e 's/^.*href=.\(.*\).$/\1/'
}

# l'url du répertoire dont il faut récupérer les noms de fichiers doit être en premier paramètre
function recursive-filenames() {
	ELTS=$(fetch-filenames "$1")
	echo "$ELTS" | grep -v "/$" | while read FILE; do
		echo "$1$FILE"
	done
	echo "$ELTS" | grep "/$" | while read DIR; do
		recursive-filenames "$1$DIR"
	done
}

MACHINE=logapp.etat-de-vaud.ch
URL="https://$MACHINE/unireg/$ENVIRONMENT/unireg-web/$REPOSITORY_DIR/"

PREFIX_LENGTH=$((${#URL} - ${#REPOSITORY_DIR} - 1))
recursive-filenames "$URL" | while read FILE; do
	RELATIVE_FILENAME="${FILE:$PREFIX_LENGTH}"
	RELATIVE_DIR=$(dirname "$RELATIVE_FILENAME")
	if [ ! -d "$RELATIVE_DIR" ]; then
		mkdir -p "$RELATIVE_DIR"
	fi
	if [ ! -e "$RELATIVE_FILENAME" ]; then
		wget --no-check-certificate "$FILE" -O "$RELATIVE_FILENAME"
		echo "Fichier $RELATIVE_FILENAME récupéré"
	else
		echo "Fichier $RELATIVE_FILENAME déjà connu" >&2
	fi
done
