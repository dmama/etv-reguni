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
	if (name == 'TacheEnvoiDeclarationImpotPP' || name == 'TacheEnvoiDeclarationImpotPM' || name == 'TacheAnnulationDeclarationImpot') {
		$('#periode_fiscale_label').show();
		$('#periode_fiscale_input').show();
		$('#commentaire_ctrl_label').hide();
		$('#commentaire_ctrl_input').hide();
	}
	else if (name == 'TacheControleDossier') {
		$('#periode_fiscale_label').hide();
		$('#periode_fiscale_input').hide();
		$('#commentaire_ctrl_label').show();
		$('#commentaire_ctrl_input').show();
	}
	else {
		$('#periode_fiscale_label').hide();
		$('#periode_fiscale_input').hide();
		$('#commentaire_ctrl_label').hide();
		$('#commentaire_ctrl_input').hide();
	}
}

/*
* Afficher alerte lors de l'impression de nouveaux dossiers
*/
function confirmeImpression() {

	var lignesNouveauDossier = document.getElementById('nouveauDossier').getElementsByTagName('tr');
	var taille = lignesNouveauDossier.length;

	var nbSelectionnes = 0;
	for(var i=1; i < taille; i++) {
		var sel = $('#tabIdsDossiers_' + i);
		if (sel && !sel.attr('disabled') && sel.attr('checked')) {
			++ nbSelectionnes;
		}
	}
	if (nbSelectionnes == 0) {
		alert('Veuillez selectionner au moins un dossier à inclure');
		return false;
	}

	$('#desynchro').show();
	return true;
}	

/*
* Cacher alerte lors de l'impression de nouveaux dossiers
*/
function recherche() {
	$('#desynchro').hide();
	var form = $("form[name=\"theForm\"]");
	form.submit();
}	
