
/**
 * @return <b>true</b> si l'application courante est déployée en développement
 */
function is_dev_env() {
	var url = this.location.toString();
	// toutes urls sur les ports 7001 (weblogic) ou 8080 (tomcat) sont considérées comme "développement"
	return url.match(/http:\/\/[.\w]+:7001\//) || url.match(/http:\/\/[.\w]+:8080\//); 
}

/**
 * Demande la confirmation à l'utilisateur avant de détruire les données de la
 * base.
 * 
 * @return <b>true</b> si l'utilisateur veut continuer, <b>false</b> autrement.
 */
function confirm_trash_db() {
	if (is_dev_env()) {
		// A priori, un développeur sait ce qu'il fait...
		return true;
	}
	return confirm('Attention ! Cette opération va détruire les données existantes de la base.\n\nVoulez-vous vraiment continuer ?');
}
