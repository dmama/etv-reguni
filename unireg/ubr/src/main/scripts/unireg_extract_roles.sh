#! /bin/bash -
# Script d'extraction du batch des rôles (communes ou OID) : extraire tous les fichiers CSV du rapport d'exécution
# et zipper les fichiers par commune / OID
#
# Paramètres :
#   - le nom du fichier pdf du rapport d'exécution du batch
#   - le répertoire dans lequel doivent être placés les fichiers zip générés

PDF="$1"
DEST="$2"
if [ -z "$PDF" -o -z "$DEST" ]; then
	echo "Syntaxe : $(basename "$0") rapport.pdf dest-dir" >&2
	echo "   où rapport.pdf est le fichier PDF rapport d'exécution du batch des rôles généré par Unireg" >&2
	echo "   et dest-dir est le chemin d'un répertoire existant dans lequel seront déposés les fichiers zip" >&2
	exit 1
elif [ ! -r "$PDF" -o ! -e "$PDF" ]; then
	echo "Impossible de lire le fichier rapport d'exécution $PDF" >&2
	exit 1
elif [ ! -d "$DEST" -o ! -w "$DEST" ]; then
	echo "Répertoire de destination '$DEST' inexistant ou inaccessible en écriture" >&2
	exit 1
fi

# Vérifions tout d'abord que nous sommes au bon endroit
DIR=$(dirname "$0")
EXTRACTOR="$DIR/unireg_rapport_csv.sh"
if [ ! -x "$EXTRACTOR" ]; then
	echo "Le script $(basename "$EXTRACTOR") devrait se trouver dans le même répertoire que ce script-ci" >&2
	exit 1
fi

# Création d'un répertoire temporaire dans lequel nous allons extraire tous les fichiers CSV du rapport d'exécution
TMP_DIR=$(mktemp -d)

# on rend le chemin vers le répertoire DEST absolu
DEST=$(cd "$DEST" && pwd)

# Extraction des fichiers CSV dans ce répertoire temporaire
"$EXTRACTOR" "$PDF" -command list | xargs "$EXTRACTOR" "$PDF" -command extract -outputdir "$TMP_DIR" -csvfiles
NB_CSV=$(ls -1 "$TMP_DIR"/*_role_pp*.csv | wc -l)
echo "$NB_CSV fichier(s) CSV de rôles ont été extraits du rapport d'exécution $PDF" >&2

# Pas besoin d'aller plus loin si aucun fichier CSV trouvé
if [ "$NB_CSV" -eq 0 ]; then
	echo "Aucun fichier CSV trouvé, on s'arrête là..." >&2
	rm -rf "$TMP_DIR"
	exit 2
fi

# Identification des communes / oid : les noms des fichiers sont de la forme ID_role_pp_AAAA[-X].csv
(cd "$TMP_DIR" && ls -1 *_role_pp*.csv) | sed -e 's/_.*$//' | sort -n | uniq | while read ID; do
	FILES=$(cd "$TMP_DIR" && ls -1 *_role_pp_*.csv | grep "^$ID")
	ZIP_FILE=$(echo "$FILES" | sed -e '2,$ D' -e 's/-[0-9]\{1,\}//' -e 's/csv$/zip/')
	if [ -e "$DEST/$ZIP_FILE" ]; then
		echo "Le fichier $ZIP_FILE existe déjà dans le répertoire de destination : il NE sera PAS écrasé!" >&2
	elif [ -n "$FILES" ]; then
		(cd "$TMP_DIR" && echo "$FILES" | xargs zip -q "$DEST/$ZIP_FILE")
	fi
done

rm -rf "$TMP_DIR"
