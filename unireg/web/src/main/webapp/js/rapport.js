/**
* Affiche le taux d'activité seulement si le type d'activité est PRINCIPAL
*/
function selectTypeActivite(type)
{
	var divTauxActiviteLabel 	= document.getElementById('tauxActiviteLabel');
  	var divTauxActiviteInput 	= document.getElementById('tauxActiviteInput');
	var form = document.getElementById('formModifRapport');
  	if (type == 'PRINCIPALE') {
	  	divTauxActiviteLabel.style.display = '';
	  	divTauxActiviteInput.style.display = '';
  	} else {
	  	divTauxActiviteLabel.style.display = 'none';
	  	divTauxActiviteInput.style.display = 'none';
	  	form.tauxActivite.value = '';
  	}
}

/**
* Switch rapport
*/
function selectSensRapport(name) {

	var divObjet 			= document.getElementById('div_objet');	
	var divSujet 			= document.getElementById('div_sujet');	

	if( name == 'objet' ){
		divObjet.style.display = '';
		divSujet.style.display = 'none';
	}
	if( name == 'sujet' ){
		divObjet.style.display = 'none';
		divSujet.style.display = '';
	}
}

/**
* Popup de confirmation de sauvegarde du tiers avant de quitter la page
*/ 
function retourRapport(numero) {
	if(confirm('Voulez-vous quitter cette page sans sauver ?')) {
		document.location.href='search.do?numero=' + numero ;
	}
}