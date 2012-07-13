#!/bin/sh

echo "==> cleanup workspace..."
rm -rf csv_orig
rm -rf csv_utf8
rm -rf csv_filtered
rm -rf tmp
mkdir -p csv_orig
mkdir -p csv_utf8
mkdir -p csv_filtered
mkdir -p tmp

echo "==> extracting CSV files from report..."
./unireg_rapport_csv.sh $1 -command extract -outputdir csv_orig

# converti les fichiers en UTF-8
echo "==> converting CSV files to UTF-8..."
for file in `cd csv_orig && ls -1 *.csv`; 
do
  iconv -f ISO-8859-1 -t UTF-8 csv_orig/$file > csv_utf8/$file;
done

# supprime les lignes d'entête
echo "==> deleting headers from CSV files..."
sed -i -s 1d csv_utf8/*.csv

# supprime les retours de lignes dans la colonne 'adresse courrier' parce que ça empêche du faire du grep correctement
echo "==> deleting multilines fields from CSV files..."
sed -i -s ':a;N;$!ba;s/"[^"]*\n[^"]*"/,/g' csv_utf8/*.csv

# crée les scripts sed des contribuables à ignorer (splittés par lot de 1'000 numéro pour des raisons de perfs)
echo "==> building filtering scripts..."
(cd tmp && split -l 1000 ../$2 script_) # split fichiers de 1'000 numéros
sed -i -s ':a;N;$!ba;s/\n/|/g' tmp/* # remplacement des retours de ligne par des |
sed -i -s 's!^!/!' tmp/* # syntaxe de delete de line sed (voir http://en.kioskea.net/faq/1451-sed-delete-one-or-more-lines-from-a-file)
sed -i -s 's!$!/d!' tmp/*

# supprime toutes les lignes des fichiers csv qui correspondent aux contribuables à ignorer
echo "==> removing unwanted taxpayer from CSV files..."
cp csv_utf8/*.csv csv_filtered
for pattern in `ls -1 tmp/*`; 
do
  echo "    - running" $pattern "script..."
  sed -i -s -r -f $pattern csv_filtered/*.csv
done


echo "==> building stats..."

ctb_traites=0
ctb_ignores=`cat csv_filtered/contribuables_ignores.csv | wc -l`
ctb_en_erreur=`cat csv_filtered/contribuables_en_erreur.csv | wc -l`

for file in `ls -1 csv_filtered/*_role_pp_*.csv`; 
do
  ctb_traites=$((ctb_traites + `cat $file | wc -l`))
done

echo ""
echo "-- Résumé général --"
echo "Nombre de contribuables traités :" $ctb_traites
echo "Nombre de contribuables ignorés :" $ctb_ignores
echo "Nombre de contribuables en erreur :" $ctb_en_erreur
echo ""

lausanne_total=0
lausanne_ordinaire=0
lausanne_depense=0
lausanne_hc=0
lausanne_hs=0
lausanne_source=0
lausanne_nonass=0

for file in `ls -1 csv_filtered/5586_*.csv`; 
do
  lausanne_total=$((lausanne_total + `cat $file | wc -l`));
  lausanne_ordinaire=$((lausanne_ordinaire + `grep -c 'ordinaire' $file`))
  lausanne_depense=$((lausanne_depense + `grep -c 'dépense' $file`))
  lausanne_hc=$((lausanne_hc + `grep -c 'hors canton' $file`))
  lausanne_hs=$((lausanne_hs + `grep -c 'hors Suisse' $file`))
  lausanne_source=$((lausanne_source + `grep -c 'source' $file`))
  lausanne_nonass=$((lausanne_nonass + `grep -c 'Non assujetti' $file`))
done

echo "-- Pour la commune de Lausanne --"
echo "Nombre de contribuables total :" $lausanne_total
echo "Nombre de contribuables ordinaires :" $lausanne_ordinaire
echo "Nombre de contribuables à la dépense :" $lausanne_depense
echo "Nombre de contribuables hors-canton :" $lausanne_hc
echo "Nombre de contribuables hors-Suisse :" $lausanne_hs
echo "Nombre de contribuables à la source :" $lausanne_source
echo "Nombre de contribuables dont l'assujettissement n'est pas poursuivi :" $lausanne_nonass


echo ""
echo "==> Done."

