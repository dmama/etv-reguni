#! /bin/bash -

# [tiers2.read] INFO  [2011-04-12 00:37:13.575] [sipf] (0 ms) GetTiers{login=UserLogin{userId='zaizzp', oid=22}, tiersNumber=0, date=Date{year=2011, month=4, day=12}, parts=[SITUATIONS_FAMILLE, CAPITAUX, FORS_FISCAUX, ADRESSES, REGIMES_FISCAUX, ETATS_PM, ADRESSES_ENVOI, COMPTES_BANCAIRES, FORS_GESTION, COMPOSANTS_MENAGE, ASSUJETTISSEMENTS, FORMES_JURIDIQUES, SIEGES]} load=1
# [tiers2.read] INFO  [2011-04-12 00:42:13.785] [sipf] (0 ms) GetTiers{login=UserLogin{userId='zaizzp', oid=22}, tiersNumber=0, date=Date{year=2011, month=4, day=12}, parts=[SITUATIONS_FAMILLE, CAPITAUX, FORS_FISCAUX, ADRESSES, REGIMES_FISCAUX, ETATS_PM, ADRESSES_ENVOI, COMPTES_BANCAIRES, FORS_GESTION, COMPOSANTS_MENAGE, ASSUJETTISSEMENTS, FORMES_JURIDIQUES, SIEGES]} load=1
# [tiers2.read] INFO  [2011-04-12 00:47:13.640] [sipf] (0 ms) GetTiers{login=UserLogin{userId='zaizzp', oid=22}, tiersNumber=0, date=Date{year=2011, month=4, day=12}, parts=[SITUATIONS_FAMILLE, CAPITAUX, FORS_FISCAUX, ADRESSES, REGIMES_FISCAUX, ETATS_PM, ADRESSES_ENVOI, COMPTES_BANCAIRES, FORS_GESTION, COMPOSANTS_MENAGE, ASSUJETTISSEMENTS, FORMES_JURIDIQUES, SIEGES]} load=1

# On va essayer de sortir le compte d'appel, par appelant et méthode appelée
awk '{ print $5 " " $8; }' "$@" | sed -e 's/{.*$//' | sort | uniq -c
