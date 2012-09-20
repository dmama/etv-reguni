#! /bin/bash -
# Tous les scripts de ce répertoire vont chercher les logs sur un serveur HTTPS protégé par une basic-authentication basée
# sur le LDAP - pour ce faire, on indique dans le fichier $HOME/.netrc le nom d'utilisateur et le mot de passe à utiliser
# par machine destinatrice (le programme wget sait faire usage de ces informations)
#
# Ce script prend donc les informations présentes dans le fichier smbcreds.xsijde (à modifier éventuellement, bien sûr) qui
# sert de fichier de credentials pour les mounts cifs, et copie le mot de passe dans le fichier $HOME/.netrc et le fichier $HOME/.wgetrc (pour le proxy)
#
# Cela permet donc de ne maintenir ce mot de passe qu'à un endroit et de l'utiliser pour plusieurs choses (mount, wget...)

USR=xsijde
CRED_FILE=$(dirname "$0")/smbcreds.$USR
if [ ! -r "$CRED_FILE" ]; then
	echo "Fichier inaccessible : $CRED_FILE" >&2
	exit 1
fi

# extraction du password
PWD=$(grep "^password=" "$CRED_FILE" | sed -e 's/^password=//')

# modification de ~/.netrc
NETRC_FILE=~/.netrc
MACHINE=logapp.etat-de-vaud.ch
sed -i -e "/machine $MACHINE/,+2 D" "$NETRC_FILE"
touch "$NETRC_FILE"
chmod 600 "$NETRC_FILE"
echo -e "machine $MACHINE\n\tlogin $USR\n\tpassword $PWD" >> "$NETRC_FILE"

## modification de ~/.wgetrc (proxy authentication)
#WGETRC_FILE=~/.wgetrc
#sed -i -e "/^proxy-\(user\|password\)=/ D" "$WGETRC_FILE"
#echo -e "proxy-user=$USR\nproxy-password=$PWD" >> "$WGETRC_FILE"
#
## modification de /etc/apt/apt.conf.d/00proxy
#APT_CONF_PROXY_FILE=/etc/apt/apt.conf.d/00proxy
#echo "Acquire::http::Proxy \"http://$USR:$PWD@webproxy.vd.ch:8080/\";" > "$APT_CONF_PROXY_FILE"
#
## modification du fichier ~/.ssh/proxy-auth
#AUTH_PROXY_SSH_FILE=~/.ssh/proxy-auth
#echo "$USR:$PWD" > "$AUTH_PROXY_SSH_FILE"

# modification du fichier /etc/cntlm.conf
CNTLM_CONF_FILE=/etc/cntlm.conf
#NEW_LINES=$(echo "$PWD" | cntlm -H -u $USR -d ADACV -a NTLMv2 | sed -e '/^Password:/ D' | tac | sed -e '2,$ s/$/\\/' | tac)
NEW_LINES=$(echo "$PWD" | cntlm -H -u $USR -d ADACV -a NTLMv2 | grep "NTLMv2" | sed -e '/^Password:/ D' | tac | sed -e '2,$ s/$/\\/' | tac)
BEGIN_LINE="### PWD-BEGIN"
END_LINE="### PWD-END"
sudo sed -i "$CNTLM_CONF_FILE" -e "/^$BEGIN_LINE/,/^$END_LINE/ c\
$BEGIN_LINE (for autoupdate)\\
$NEW_LINES\\
$END_LINE
"

# redémarrage du proxy local
echo "Redémarrage du web-proxy local..."
sudo service cntlm reload
