#! /bin/bash -
# Post-processing des logs de migration (= surtout des fichiers CSV) pour
# 1. les découper en morceaux de taille appréhendable
# 2. en extraire séparément les WARN et les ERROR

LOGDIR=$1
DESTFILE=$2
if [ -z "$LOGDIR" -o -z "$DESTFILE" ]; then
        echo "Syntaxe : $(basename "$0") logdir migration-logs.7z" >&2
        echo "          où logdir représente le répertoire contenant les logs d'une migration" >&2
        echo "          et migration-logs.7z est le nom du fichier (format 7z) contenant les logs post-processés." >&2
        exit 1
elif [ ! -d "$LOGDIR" ]; then
        echo "Le chemin '$LOGDIR' ne correspond pas à un répertoire." >&2
        exit 2
fi

# fonction de recopie de la première ligne (= nom des colonnes), le nom du fichier de sortie est dans "$FILE"
function avec_premiere_ligne() {

        # d'un fichier "TOTO-xx.csv", on doit retrouver "TOTO-columns.csv", car c'est de là que la première ligne doit venir
        local COLFILE=${FILE/%-??.csv/-columns.csv}

        head -n 1 "$COLFILE" > "$FILE"
        cat - >> "$FILE"
}
export -f avec_premiere_ligne


# un répertoire de travail temporaire (= destination des fichiers retravaillés)
WORKDIR=$(mktemp -d)


#
# Découpage et ré-encodage des fichiers csv
#

# on va traiter les fichiers csv un par un
(cd "$LOGDIR" && find . -type f -name "*.csv") | while read LOGFILE; do

        COREFILE=${LOGFILE/%.csv/}
        COREDIR=$(basename "$COREFILE")

        # on crée le répertoire destination
        mkdir -p "$WORKDIR/$COREDIR"

        # avancée du traitement
        echo "Ré-encodage et découpage du fichier $LOGDIR/$LOGFILE vers $COREDIR"

        # recopie de la première ligne (= les noms des colonnes) dans un fichier ad'hoc
        COLUMNFILE="$WORKDIR/$COREDIR/$COREDIR-columns.csv"
        head -n 1 "$LOGDIR/$LOGFILE" | iconv -c -t ISO88591 > "$COLUMNFILE"

        # les utilisateurs préfèrent l'encodage ISO-8859-1 (pour Excel), et des fichiers de 100'000 lignes au maximum
        tail -n +2 "$LOGDIR/$LOGFILE" | iconv -c -t ISO88591 | split -l 100000 --additional-suffix=.csv --numeric-suffixes=1 --filter=avec_premiere_ligne - "$WORKDIR/$COREDIR/$COREDIR-"

        # on veut aussi produire des fichiers additionnels avec les WARN et les ERROR
        for LVL in WARN ERROR; do

                # fichier extrait
                LVLFILE="$WORKDIR/$COREDIR/$COREDIR-$LVL.csv"

                # recopie de la liste des colonnes
                cat "$COLUMNFILE" > "$LVLFILE"

                # les données elles-mêmes
                find "$WORKDIR/$COREDIR" -type f -maxdepth 1 -name "$COREDIR-??.csv" | sort | while read BRUTTO; do
                        grep "^${LVL};" "$BRUTTO"
                done >> "$LVLFILE"

                # si le fichier n'a qu'une ligne, il peut sauter
                NBLINES=$(cat "$LVLFILE" | wc -l)
                if [ "$NBLINES" -eq 1 ]; then
                        rm "$LVLFILE"
                fi
        done

        # c'était un fichier temporaire
        rm -f "$COLUMNFILE"
done

# recopie des fichiers non-csv + encodage
(cd "$LOGDIR" && find . -type f ! -name "*.csv") | while read NOTCSV; do

        # création de l'éventuel répertoire additionnel
        NOTCSVDIR=$(dirname "$NOTCSV")
        mkdir -p "$WORKDIR/$NOTCSVDIR"

        # ne pas oublier l'encodage
        iconv -c -t ISO88591 "$LOGDIR/$NOTCSV" > "$WORKDIR/$NOTCSV"

done


#
# Extractions spécifiques
#

# les fors principaux adaptés pour couvrir les fors secondaires
(
	echo "Extraction des fors principaux adaptés à la couverture des fors secondaires..."
	INPUT_FILE="$LOGDIR/fors.csv"
	OUTPUT_FILE="$WORKDIR/fors/fors-principaux-adaptes-pour-couverture-fors-secondaires.csv"
	head -n 1 "$INPUT_FILE" | iconv -c -t ISO88591 > "$OUTPUT_FILE"
	grep "date de \(début\|fin\) .* couvrir les fors secondaires" "$INPUT_FILE" | iconv -c -t iso88591 >> "$OUTPUT_FILE"
)


# construction du fichier d'export
echo "Construction de l'archive $DESTFILE..."
CURDIR=$(pwd)
rm -f "$DESTFILE"
if [[ "$DESTFILE" =~ ^[^/].* ]]; then
        DESTFILE="$PWD/$DESTFILE"
fi
(cd "$WORKDIR" && 7z a "$DESTFILE" *)

# nettoyage des fichiers temporaires
rm -rf "$WORKDIR"
