#! /bin/bash -
# Récupère les fichiers de logs des 20 derniers jours (en tout cas ceux que nous n'avons pas encore)
# et compresse les fichiers de logs plus vieux que 10 jours (algorithme LZMA)

ENVIRONMENT=$1
OLD_NEW=$2
if [ -z "$ENVIRONMENT" ]; then
        echo "Syntaxe : $(basename "$0") <env> [old] avec <env> l'un de PR, PO, VA, PP, FO" >&2
        exit 1
elif [[ ! "$ENVIRONMENT" =~ ^(PR|PO|VA|PP|FO)$ ]]; then
        echo "Pour l'environnement, seuls PR, PO, VA, PP et FO sont acceptés (trouvé : '$ENVIRONMENT')" >&2
        exit 1
elif [ -n "$OLD_NEW" -a "$OLD_NEW" != "old" ]; then
        echo "Syntaxe : $(basename "$0") <env> [old] avec <env> l'un de PR, PO, VA, PP, FO" >&2
        exit 1
fi

UNE_SEULE_WEB_APP=""
if [ -n "$OLD_NEW" ]; then
	UNE_SEULE_WEB_APP="Y"
fi

MACHINE=logapp.etat-de-vaud.ch
DATE_DEBUT=$(date --date "20 days ago" +"%Y%m%d")
TODAY=$(date +"%Y%m%d")
TODAY_SEC=$(date --date "$TODAY" +"%s")

function fetch-logs() {

	PREFIXE_FILE="$1"
	PREFIXE_URL="$2"

	DATE_COURANTE=$DATE_DEBUT
	while test "$DATE_COURANTE" -lt "$TODAY"; do

		DATE_COURANTE_SEC=$(date --date "$DATE_COURANTE" +"%s")
		SUFFIXE_DATE_COURANTE=$(echo $DATE_COURANTE | sed -e 's/^\([[:digit:]]\{4\}\)\([[:digit:]]\{2\}\)\([[:digit:]]\{2\}\)$/\1-\2-\3/')
		FILE=$PREFIXE_FILE$SUFFIXE_DATE_COURANTE
		if [ ! -e "$FILE" -a ! -e "$FILE.lzma" ]; then
			echo "Fichier $FILE n'existe pas encore, allons le chercher"
			wget --no-proxy --no-check-certificate $PREFIXE_URL$FILE -O "$FILE"
			if [ "$?" -ne 0 ]; then
				rm -f "$FILE"
			fi
		fi
		if [ -e "$FILE" -a $(($TODAY_SEC - $DATE_COURANTE_SEC)) -gt 864000 ]; then
			echo "Compression de vieux fichier $FILE"
			lzma "$FILE"
		fi

		DATE_COURANTE=$(date --date "$DATE_COURANTE + 1 day" +"%Y%m%d")
	done
}

if [ -n "$UNE_SEULE_WEB_APP" ]; then

	# les fichiers sont téléchargés dans un sous-répertoire spécifique à l'environnement voulu
	cd "$(dirname "$0")/$ENVIRONMENT" && fetch-logs unireg-web.log. "https://$MACHINE/unireg/$ENVIRONMENT/logs/"

else

	# pour l'instant, aucun environnement n'a plusieurs tomcats
	TOMCAT_PART="-cat_unireg${ENVIRONMENT}01"

	# partie web
	echo "Webapp WEB"
	(cd "$(dirname "$0")/$ENVIRONMENT/unireg-web" && fetch-logs "unireg-web.log." "https://$MACHINE/unireg/$ENVIRONMENT/unireg-web/logs/")
	(cd "$(dirname "$0")/$ENVIRONMENT/unireg-web" && fetch-logs "srv-access-web.log." "https://$MACHINE/unireg/$ENVIRONMENT/unireg-web/logs/")
	(cd "$(dirname "$0")/$ENVIRONMENT/unireg-web" && fetch-logs "unireg-web${TOMCAT_PART}.log." "https://$MACHINE/unireg/$ENVIRONMENT/unireg-web/logs/")
	(cd "$(dirname "$0")/$ENVIRONMENT/unireg-web" && fetch-logs "srv-access-web${TOMCAT_PART}.log." "https://$MACHINE/unireg/$ENVIRONMENT/unireg-web/logs/")
	(cd "$(dirname "$0")/$ENVIRONMENT/unireg-web" && fetch-logs "web-access${TOMCAT_PART}.log." "https://$MACHINE/unireg/$ENVIRONMENT/unireg-web/logs/")

	# partie WS
	echo "Webapp WS"
	(cd "$(dirname "$0")/$ENVIRONMENT/unireg-ws" && fetch-logs "unireg-ws.log." "https://$MACHINE/unireg/$ENVIRONMENT/unireg-ws/logs/")
	(cd "$(dirname "$0")/$ENVIRONMENT/unireg-ws" && fetch-logs "ws-access.log." "https://$MACHINE/unireg/$ENVIRONMENT/unireg-ws/logs/")
	(cd "$(dirname "$0")/$ENVIRONMENT/unireg-ws" && fetch-logs "srv-access-ws.log." "https://$MACHINE/unireg/$ENVIRONMENT/unireg-ws/logs/")
	(cd "$(dirname "$0")/$ENVIRONMENT/unireg-ws" && fetch-logs "unireg-ws${TOMCAT_PART}.log." "https://$MACHINE/unireg/$ENVIRONMENT/unireg-ws/logs/")
	(cd "$(dirname "$0")/$ENVIRONMENT/unireg-ws" && fetch-logs "ws-access${TOMCAT_PART}.log." "https://$MACHINE/unireg/$ENVIRONMENT/unireg-ws/logs/")
	(cd "$(dirname "$0")/$ENVIRONMENT/unireg-ws" && fetch-logs "srv-access-ws${TOMCAT_PART}.log." "https://$MACHINE/unireg/$ENVIRONMENT/unireg-ws/logs/")
	
fi
