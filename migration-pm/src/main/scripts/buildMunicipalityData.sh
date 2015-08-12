#! /bin/bash -
# Prend en paramètre (ou sur l'entrée standard) un fichier XML résultat de la requête /ws/v5/listOfMunicipalities de RefInf et en génère un fichier CSV
# utilisable dans la migration (voir variable de configuration "fusions.communes.file")

function decoupage_balises {
	sed -e 's/>/>\n/g' "$@" | sed -e '/[^>]$/ N;s/\n//'
}

function extraction_id_dates {
	grep "municipality\(Id\|AbolitionNumber\|AdmissionNumber\|AbolitionDate\|AdmissionDate\)" | grep "^[0-9]\+" | sed -e 's/\([0-9-]\+\).*municipality\(.*\)$/\1 \2/'
}

function regroupement_mutation_date {
	sed -e '/Number/ N;s/\n/ /' | awk '/Id/ { print $1; } /Admission/ { print "created " $1 " " $3; } /Abolition/ { print "removed " $1 " " $3; }' | grep -v "created 1000\b"
}

function regroupement_ligne {
	awk '{ if ($1 ~ /^[0-9]/) print "\n\n"$1; else print $0; }' | sed -e '/^[0-9]/ N;s/\n/ /' -e '/^[0-9]/ N;s/\n/ /' | grep "[0-9]"
}

function formattage_csv {
	sed -e '/created.*removed/ s/[^0-9-]\+/;/g' -e '/created/ { s/[^0-9-]\+/;/g;s/;*$/;;/ }' -e '/removed/ { s/ removed /;;;/g;s/[^0-9;-]/;/ }' -e '/^[0-9 ]\+$/ s/ *$/;;;;/' -e 's/ \+$//'
}

echo "NO_OFS;MUTATION_CREATION;DATE_MUTATION_CREATION;MUTATION_DISPARITION;DATE_MUTATION_DISPARITION"
decoupage_balises "$@" | extraction_id_dates | regroupement_mutation_date | regroupement_ligne | formattage_csv
