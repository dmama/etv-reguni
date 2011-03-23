#!/bin/sh
# Ce script extrait les statistiques d'accès par user technique <-> user business 
grep "tiers2.read" $* | awk '{ print $5, $8 }' | sed -E "s/\[(.*)\].*'(.*)'.*/\1 \2/" | sort | uniq -c | sort -nr
