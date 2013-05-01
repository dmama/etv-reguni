#! /bin/bash -
# Détruit les fichiers ".incremental.lock" posés pendant leur exécution par les scripts "fetch-today-..."
# Ce script est sensé être lancé à un moment où les "fetch-today-..." ne peuvent pas tourner pour nettoyer un crash éventuel

find "$(dirname "$0")" -type f -name ".incremental.lock" -exec rm -f {} \;
