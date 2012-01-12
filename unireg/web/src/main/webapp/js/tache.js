/**
* Selectionne / Deselectionne tous les dossiers
*/
function selectAllDossiers(checkSelectAll) {

	var lignesNouveauDossier = document.getElementById('nouveauDossier').getElementsByTagName('tr');
	var taille = lignesNouveauDossier.length;

	for(var i=1; i < taille; i++) {
		$('#tabIdsDossiers_' + i).attr('checked', checkSelectAll.checked);
	}
}

/*
* Selection d'un type de tache
*/
function selectTypeTache(name) {
	if( name == 'TacheEnvoiDeclarationImpot' || name == 'TacheAnnulationDeclarationImpot' ){
		$('#periode_fiscale_label').show();
		$('#periode_fiscale_input').show();
	} else {
		$('#periode_fiscale_label').hide();
		$('#periode_fiscale_input').hide();
	}
}

/*
* Afficher alerte lors de l'impression de nouveaux dossiers
*/
function confirmeImpression() {
	$('#desynchro').show();
	var form = $("form[name=\"theForm\"]");
	form.attr('action', 'list-nouveau-dossier.do?imprimer=imprimer');
	form.submit();
}	

/*
* Cacher alerte lors de l'impression de nouveaux dossiers
*/
function recherche() {
	$('#desynchro').hide();
	var form = $("form[name=\"theForm\"]");
	form.attr('action', 'list-nouveau-dossier.do');
	form.submit();
}	

/*
* Cacher alerte lors de l'impression de nouveaux dossiers
*/
function efface() {
	$('#desynchro').hide();
	var form = $("form[name=\"theForm\"]");
	form.attr('action', 'list-nouveau-dossier.do?effacer=effacer');
	form.submit();
}	
