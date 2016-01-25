#! /bin/bash -
# Permet de migrer une arborescence de fichiers de log depuis l'époque où unireg n'était composé que d'une webapp
# vers l'ère où la webapp WS est apparue

ENVIRONMENT=$1
if [ -z "$ENVIRONMENT" ]; then
        echo "Syntaxe : $(basename "$0") <env> avec <env> l'un de PR, PO, VA, PP, FO" >&2
        exit 1
elif [[ ! "$ENVIRONMENT" =~ ^(PR|PO|VA|PP|FO)$ ]]; then
        echo "Pour l'environnement, seuls PR, PO, VA, PP et FO sont acceptés (trouvé : '$ENVIRONMENT')" >&2
        exit 1
fi

cd $(dirname "$0")/$ENVIRONMENT
if [ -d "./unireg-web" -o -d "./unireg-ws" ]; then
	echo "On dirait bien que la migration de l'environnement $ENVIRONMENT a déjà eu lieu..." >&2
	exit 1
fi

echo "Migration de l'environnement $ENVIRONMENT au passage à plusieurs web-apps (web + ws)"
echo

# déplacement des fichiers actuels dans le sous-répertoire web, création du sous-répertoire ws
mkdir -v unireg-web unireg-ws
find . -mindepth 1 -maxdepth 1 ! -name "unireg-web" ! -name "unireg-ws" | while read FILE; do
	mv -v "$FILE" unireg-web/
done
echo

# Modification du crontab pour cet environnement

echo "Modification du crontab"
echo "Avant :" && crontab -l | while read LINE; do echo -e "\t\t$LINE"; done
echo

crontab -l | sed -e "/\b$ENVIRONMENT\b/ s/old //" -e "/fetch-todays-log\.sh $ENVIRONMENT\b/ s/-log/-web-log/" | tee ~/logs/crontab.pour-info | crontab -
echo "Après :" && crontab -l | while read LINE; do echo -e "\t\t$LINE"; done
