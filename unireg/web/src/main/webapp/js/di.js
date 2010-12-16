/**
* Efface les valeurs des criteres de recherche des DI
*/
function effacerCriteresDI()
{
  top.location.replace('list.do?action=effacer');
}


/*
* Retour vers la visualistion du contribuable
*/
function retourVisuFromDI(numero) {
	document.location.href='../tiers/visu.do?id=' + numero ;
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
		
