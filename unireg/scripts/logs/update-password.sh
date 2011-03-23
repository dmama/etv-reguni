#! /bin/bash -
# Tous les scripts de ce répertoire vont chercher les logs sur un serveur HTTPS protégé par une basic-authentication basée
# sur le LDAP - pour ce faire, on indique dans le fichier $HOME/.netrc le nom d'utilisateur et le mot de passe à utiliser
# par machine destinatrice (le programme wget sait faire usage de ces informations)
#
# Ce script prend donc les informations présentes dans le fichier smbcreds.xsijde (à modifier éventuellement, bien sûr) qui
# sert de fichier de credentials pour les mounts cifs, et copie le mot de passe dans le fichier $HOME/.netrc
#
# Cela permet donc de ne maintenir ce mot de passe qu'à un endroit et de l'utiliser pour plusieurs choses (mount, wget...)

CRED_FILE=$(dirname "$0")/smbcreds.xsijde
if [ ! -r "$CRED_FILE" ]; then
	echo "Fichier inaccessible : $CRED_FILE" >&2
	exit 1
fi

NETRC_FILE=~/.netrc
MACHINE=logapp.etat-de-vaud.ch

PWD=$(grep "^password=" "$CRED_FILE" | sed -e 's/^password=//')
sed -i -e "/machine $MACHINE/,+2 D" "$NETRC_FILE"
touch "$NETRC_FILE"
chmod 600 "$NETRC_FILE"
echo -e "machine $MACHINE\n\tlogin xsijde\n\tpassword $PWD" >> "$NETRC_FILE"
