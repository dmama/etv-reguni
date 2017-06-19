#! /bin/bash -
# L'idée est de générer un zip au format attendu par l'équipe DBA du CEI pour un passage de script(s) SQL
# Syntaxe : xxx.sh unireg-web UNIREG SIFISC-8435-8715 toto.sql titi.sql
# 	où unireg-web est le nom de l'application (constante fournie par le CEI)
#	   UNIREG est le nom du schéma en base
#	   SIFISC-8435-8715 est le nom à assigner au package (donc aussi au zip)
#          *.sql sont les noms des scripts SQL à inclure dans le package (dans l'ordre indiqué)
#
# Le but est de construire un fichier ZIP avec l'arborescence suivante :
# SIFISC-8435-8715/
#	unireg-web/
#		01_UNIREG_*.sql
#		...
#		99_UNIREG_*.sql

NOM_APPLICATION="$1"
NOM_SCHEMA="$2"
NOM_PACKAGE="$3"
shift 3		# comme ça, tous les paramètres restants sont des noms de scripts SQL

if [ -z "$NOM_APPLICATION" -o -z "$NOM_PACKAGE" -o -z "$NOM_SCHEMA" -o "$#" -lt 1 ]; then
	echo "Syntaxe : $(basename "$0") nom-application nom-schéma nom-package script1.sql [script2.sql...]" >&2
	echo "Exemple : $(basename "$0") unireg-web UNIREG mon-prod-travaux script1.sql [script2.sql...]" >&2
	exit 1
fi

# détrompeur... si on a oublié un paramètre avant les scripts SQL
if [[ "$NOM_PACKAGE" =~ \.sql$ ]]; then
	echo "Syntaxe : $(basename "$0") nom-application nom-schéma nom-package script1.sql [script2.sql...]" >&2
	exit 1
fi

# vérification des noms des fichiers de scripts (le standard CEI imposent que leur extension soit .sql)
MAUVAIS_SCRIPTS=$(for SCRIPT in "$@"; do if [[ ! "$SCRIPT" =~ \.sql$ ]]; then echo "$SCRIPT"; fi; done)
if [ -n "$MAUVAIS_SCRIPTS" ]; then
	echo "Les noms des fichiers de script doivent se terminer en .sql:" >&2
	echo "$MAUVAIS_SCRIPTS" | while read LINE; do echo -e "\t$LINE"; done >&2
	exit 1
fi

# vérification que tous les fichiers de scripts sont bien accessibles en lecture
SCRIPTS_ABSENTS=$(for SCRIPT in "$@"; do if [ ! -r "$SCRIPT" ]; then echo "$SCRIPT"; fi; done)
if [ -n "$SCRIPTS_ABSENTS" ]; then
	echo "Les noms des scripts doivent correspondre à des fichiers accessibles:" >&2
	echo "$SCRIPTS_ABSENTS" | while read LINE; do echo -e "\t$LINE"; done >&2
	exit 1
fi

# construction de la structure du zip dans un répertoire temporaire
TMP_DIR=$(mktemp -d)
mkdir -p "$TMP_DIR/$NOM_PACKAGE/$NOM_APPLICATION"

# numérotation des scripts
SEQ=$(seq -w "$#" | if [ "$#" -lt 10 ]; then while read LINE; do echo "0$LINE"; done; else cat -; fi)

# histoire de mettre chaque fichier de script sur une ligne séparée
SCRIPTS=$(for SCRIPT in "$@"; do echo "$SCRIPT"; done)

# recopie des scripts dans la structure avec le bon nom et la numérotation
paste <(echo "$SEQ") <(echo "$SCRIPTS") | while read ID SCRIPT; do
	NEW_FILE="$TMP_DIR/$NOM_PACKAGE/$NOM_APPLICATION/${ID}_${NOM_SCHEMA}_$(basename "$SCRIPT")"
	cp "$SCRIPT" "$NEW_FILE"
done

# création du zip et cleanup
(cd "$TMP_DIR" && zip -9r - "$NOM_PACKAGE") > "$NOM_PACKAGE.zip" 
rm -rf "$TMP_DIR"
echo "Fichier zip créé: $NOM_PACKAGE.zip"
