/*
* Retour vers la visualistion du contribuable
*/
function retourVisuFromMvt(numero) {
	document.location.href='../tiers/visu.do?id=' + numero ;
}

/*
* Selection d'un type de mouvement
*/
function selectTypeMouvement(name) {
	if( name == 'EnvoiDossier' ){
		Element.show('envoi');
		Element.hide('reception');
	} else if( name == 'ReceptionDossier' ){
		Element.hide('envoi');
		Element.show('reception');
	} 
}

/*
* Selection d'un envoi
*/
function selectEnvoi(name) {
	if( name == 'utilisateurEnvoi' ){
		Element.show('utilisateursEnvoi');
		Element.hide('collectivites');	
	}
	if( name == 'collectivite' ){
		Element.show('collectivites');
		Element.hide('utilisateursEnvoi');	
	}
}

/*
* Selection d'une reception
*/
function selectReception(name) {
	if( name == 'PERSONNE' ){
		Element.show('utilisateursReception');	
	}
	else {
		Element.hide('utilisateursReception');
	}
}