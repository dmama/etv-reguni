#! /bin/bash -
# Extrait d'un fichier type ws-access.log les lignes qui correspondent à des temps de réponse supérieurs à la minute

grep "(\([6-9][0-9]\{4\}\|[1-9][0-9]\{5,\}\) ms)" "$@"
