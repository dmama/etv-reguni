/**
* Efface les valeurs des criteres de recherche des DI
*/
function effacerCriteresDI()
{
  top.location.replace('list.do?action=effacer');
}


/*
* Effacer les criteres d'impression de la DI
*/
function effacerImpressionDI(idDI)
{
	document.location.href='impression.do?id=' + idDI + '&action=effacer' ;
}


/*
* Retour vers la visualistion du contribuable
*/
function retourVisuFromDI(numero) {
	document.location.href='../tiers/visu.do?id=' + numero ;
}


/*
* Imprimer un duplicata de DI
*/
function duplicataDI() {
	var formImpressionDI = document.getElementById('formImpression');
	var imprimerDuplicata = document.getElementById('imprimer');
	var effacer = document.getElementById('effacer');
	imprimerDuplicata.disabled = true ;
	effacer.disabled = true ;
	formImpressionDI.action = 'impression.do?action=duplicataDI';
	formImpressionDI.submit();
}

/*
* Ajouter un delai
*/
function ajouterDelai() {
	var formAddDelai = document.getElementById('formAddDelai');
	formAddDelai.submit(); 	
}

/*
* Recharge la page avec un autre type de document
*/
function changeTypeDocument(idDI, typeDocument)
{
	document.location.href='impression.do?id=' + idDI + '&typeDocument=' + typeDocument;
}
		