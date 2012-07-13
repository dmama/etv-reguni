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

echo "==> Done."