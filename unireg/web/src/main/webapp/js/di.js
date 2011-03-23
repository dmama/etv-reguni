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
