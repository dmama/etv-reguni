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
		$('#envoi').show();
		$('#reception').hide();
	} else if( name == 'ReceptionDossier' ){
		$('#envoi').hide();
		$('#reception').show();
	} 
}

/*
* Selection d'un envoi
*/
function selectEnvoi(name) {
	if( name == 'utilisateurEnvoi' ){
		$('#utilisateursEnvoi').show();
		$('#collectivites').hide();	
	}
	if( name == 'collectivite' ){
		$('#collectivites').show();
		$('#utilisateursEnvoi').hide();	
	}
}

/*
* Selection d'une reception
*/
function selectReception(name) {
	if( name == 'PERSONNE' ){
		$('#utilisateursReception').show();	
	}
	else {
		$('#utilisateursReception').hide();
	}
}