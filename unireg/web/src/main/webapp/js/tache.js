/**
* Selectionne / Deselectionne tous les dossiers
*/
function selectAllDossiers(checkSelectAll) {

	var lignesNouveauDossier = document.getElementById('nouveauDossier').getElementsByTagName('tr');
	var taille = lignesNouveauDossier.length;

	if (checkSelectAll.checked) {
		for(var i=1; i < taille; i++) {
			if (E$('tabIdsDossiers_' + i) != null) {
				E$('tabIdsDossiers_' + i).checked = true ;
			}
		}
	} else {
		for(var i=1; i < taille; i++) {
			if (E$('tabIdsDossiers_' + i) != null) {
				E$('tabIdsDossiers_' + i).checked = false ;
			}
		}
	}
}

/*
* Selection d'un type de tache
*/
function selectTypeTache(name) {
	if( name == 'TacheEnvoiDeclarationImpot' || name == 'TacheAnnulationDeclarationImpot' ){
		Element.show('periode_fiscale_label');
		Element.show('periode_fiscale_input');
	} else {
		Element.hide('periode_fiscale_label');
		Element.hide('periode_fiscale_input');
	}
}

/*
* Afficher alerte lors de l'impression de nouveaux dossiers
*/
function confirmeImpression() {
	Element.show('desynchro');
	var form = F$("theForm");
	form.action = 'list-nouveau-dossier.do?imprimer=imprimer';
	form.submit();
}	

/*
* Cacher alerte lors de l'impression de nouveaux dossiers
*/
function recherche() {
	Element.hide('desynchro');
	var form = F$("theForm");
	form.action = 'list-nouveau-dossier.do';
	form.submit();
}	

/*
* Cacher alerte lors de l'impression de nouveaux dossiers
*/
function efface() {
	Element.hide('desynchro');
	var form = F$("theForm");
	form.action = 'list-nouveau-dossier.do?effacer=effacer';
	form.submit();
}	