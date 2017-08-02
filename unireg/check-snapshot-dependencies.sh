#! /bin/bash -

# on se place d'abord dans le bon répertoire (= celui où ce script existe)
cd "$(dirname "$0")"

# allons chercher le numéro de version Unireg courant
CURRENT_VERSION=$(grep "^long=" base/version.txt | sed -e 's/^long=//')
if [ -z "$CURRENT_VERSION" ]; then
	echo "Impossible de retrouver le numéro de version courant (depuis le fichier $(pwd)/base/version.txt)" >&2
	exit 1
fi

# lançons un calcul des dépendances SNAPSHOT (après une petite compile silencieuse histoire de tout avoir en local)
(cd base && mvn install -Pall,not) >/dev/null 2>&1
ERR_COMPILATION=$?
if [ $ERR_COMPILATION -ne 0 ]; then
	if [ $ERR_COMPILATION -gt 128 ]; then
		echo "Abandon (signal $((ERR_COMPILATION - 128)) reçu)..." >&2
	else
		echo "Il semble y avoir un souci de compilation dans le projet..." >&2
	fi
	exit $ERR_COMPILATION
fi
(cd base && mvn dependency:list) | grep SNAPSHOT | grep -v "\bBuilding Unireg - " | grep -v "uniregctb.*:${CURRENT_VERSION}:" | awk '{ print $2; }' | sort -u | while IFS=: read GROUP_ID ARTIFACT_ID FORMAT VERSION SCOPE; do
	echo "Dépendance SNAPSHOT trouvée : ${GROUP_ID}/${ARTIFACT_ID}, version ${VERSION} (scope ${SCOPE})"
done
