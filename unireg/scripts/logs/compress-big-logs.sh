#! /bin/bash -
# Compresse les logs qui font plus de 200M déjà en avance (= ne pas forcément attendre 10 jours)
find $HOME/logs -name "*.log.????-??-??" -size +200M -exec lzma {} \;
