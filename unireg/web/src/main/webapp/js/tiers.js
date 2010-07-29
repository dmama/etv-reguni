
/**
* Efface les valeurs des criteres de recherche du Host
*/
function effacerCriteres()
{
  top.location.replace('list.do?action=effacer');
}

/*
* Toggle rows is actif
*/
function toggleRowsIsHisto(tableName,elementId, numCol){
	
	var tbl = E$(tableName);
	if (tbl != null) {
		var len = tbl.rows.length;
		
		for (i=1 ; i< len; i++){
			if (!E$(elementId).checked ){		
				var x = tbl.rows[i].cells;
				if ((trim(x[numCol].innerHTML) == '') && (x[numCol].innerHTML.indexOf('strike')== -1)){
					tbl.rows[i].style.display = '';
				}else {
					tbl.rows[i].style.display = 'none';
				}
			 }  else {
			 	tbl.rows[i].style.display = '';
			 } 
		}
		
		if (tableName=='dossierApparente'){
			filter(tableName,E$('typeRapportId'));
		}
	}
}	


/*
* Toggle rows is actif
*/
function toggleRowsIsHistoPeriodicite(tableName,elementId, numCol,numColActive){

	var tbl = E$(tableName);
	if (tbl != null) {
		var len = tbl.rows.length;

		for (i=1 ; i< len; i++){
			if (!E$(elementId).checked ){
				var x = tbl.rows[i].cells;
				if (((trim(x[numCol].innerHTML) == '') && (x[numCol].innerHTML.indexOf('strike')== -1))||(x[numColActive].innerHTML.match('Active'))){
					tbl.rows[i].style.display = '';
				}else {
					tbl.rows[i].style.display = 'none';
				}
			 }  else {
			 	tbl.rows[i].style.display = '';
			 }
		}

	}
}
/*
* Toggle rows is actif
*/
function toggleRowsIsActif(tableName, elementId, numCol){
	
	var tbl = E$(tableName);
	if (tbl != null) {
		var len = tbl.rows.length;
		
		for (i=1 ; i< len; i++){
			if (!E$(elementId).checked ){		
				var x = tbl.rows[i].cells;
				if (x[numCol].innerHTML.indexOf('strike')== -1){
					tbl.rows[i].style.display = '';
				}else {
					tbl.rows[i].style.display = 'none';
				}
			 }  else {
			 	tbl.rows[i].style.display = '';
			 } 
		}
	}
}	

/*
* Filter select box
*/
function filter(tableName,element){
	var tbl = E$(tableName);		
	var sel  = element.options[element.selectedIndex].value ;
	var hide= true;
	var len = tbl.rows.length;
	var vStyle = (hide)? "none":"";

	for (i=1 ; i< len; i++){
		var x = tbl.rows[i].cells;
			
		if (tbl.rows[i].style.display == '') {
		
			if (x[0].innerHTML.match(sel) ){
				
				tbl.rows[i].style.display = '';
			} 
			
			else if(sel == 'Appartenance/Composition ménage' && (x[0].innerHTML.match('Appartenance ménage') || x[0].innerHTML.match('Composition ménage'))) {
				tbl.rows[i].style.display = '';
			}
			
			else if (sel == 'Pupille/Tuteur' && (x[0].innerHTML.match('Tuteur') || x[0].innerHTML.match('Pupille sous tutelle'))) {
				tbl.rows[i].style.display = '';
			} 
			
			else if (sel == 'Pupille/Curateur' && (x[0].innerHTML.match('Curateur') || x[0].innerHTML.match('Pupille sous curateur'))) {
				tbl.rows[i].style.display = '';
			} 
			
			else if (sel == 'Sous conseil légal/Conseil légal' && (x[0].innerHTML.match('Conseil légal') || x[0].innerHTML.match('Sous conseil légal'))) {
				tbl.rows[i].style.display = '';
			} 
			
			else if (sel == 'Enfant/Parent' && (x[0].innerHTML.match('Enfant') || x[0].innerHTML.match('Parent'))) {
				tbl.rows[i].style.display = '';
			}
			
			else if (sel == 'Représenté/Représentant' && (x[0].innerHTML.match('Représenté') || x[0].innerHTML.match('Représentant'))) {
				tbl.rows[i].style.display = '';
			}
			
			else if (sel == 'tous'){
				tbl.rows[i].style.display = '';
			} else {
				tbl.rows[i].style.display = vStyle;
			}
		}
	}
}

/*
* Annuler un rapport
*/
function annulerRapport(idRapport) {
	if(confirm('Voulez-vous vraiment annuler ce rapport entre tiers ?')) {
		var form = F$("theForm");
		form.doPostBack("annulerRapport", idRapport);
 	}
}

/*
 * Annuler un rapport entre un débiteur et un sourcier
 */
function annulerRapportPrestation(idRapport, refreshCurrentPageAfterwards) {
	if(confirm('Voulez-vous vraiment annuler ce rapport entre tiers ?')) {
		var form = document.createElement("form");
		form.action = getContextPath() + "/rapports-prestation/edit.do";
		form.method = 'POST';

		var target = document.createElement('input');
		target.name = '__TARGET__';
		target.value = 'annulerRapport';
		form.appendChild(target);

		var argument = document.createElement('input');
		argument.name = '__EVENT_ARGUMENT__';
		argument.value = idRapport;
		form.appendChild(argument);

		if (refreshCurrentPageAfterwards) {
			var retour = document.createElement('input');
			retour.name = '__URL_RETOUR__';
			retour.value = window.location;
			form.appendChild(retour);
		}

		form.submit();
 	}
}

/**
* Popup de confirmation de sauvegarde du tiers avant de quitter la page
*/ 
function retourRT(numeroSrc, numeroDpi) {
	if(confirm('Voulez-vous quitter cette page sans sauver ?')) {
		if (numeroSrc != null) {
			document.location.href='list-debiteur.do?numeroSrc=' + numeroSrc ;
		}
		if (numeroDpi != null) {
			document.location.href='list-sourcier.do?numeroDpi=' + numeroDpi ;
		}
	}
}

/**
* Reprise d'adresse
*/ 
function reprise(mode,index) {
	
		var form = document.getElementById('formAddAdresse');
		form.mode.value =	mode ;
		form.index.value = index ;
		form.action = 'adresse.do';		
		form.submit();
}	

/*
* Selection d'une periode de decompte
*/
function selectPeriodeDecompte(name) {
	if( name == 'UNIQUE' ){
		Element.show('div_periodeDecompte_label');
		Element.show('div_periodeDecompte_input');
	} else {
		Element.hide('div_periodeDecompte_label');
		Element.hide('div_periodeDecompte_input');
	} 
}


/**
* Affichage des adresses actives ou pas
*/ 
function afficheAdressesHisto(elementId,elementIdCiviles,elementIdCivilesConjoint, numero) {
	var histo;
	var histoCiviles;
	var histoCivilesConjoint;

	if (E$(elementId).checked ){
		histo = '&adressesHisto=true';
	}
	else {
		histo =  '&adressesHisto=false';
	}

	if (E$(elementIdCiviles).checked ){
		histoCiviles = '&adressesHistoCiviles=true';
	}
	else {
		histoCiviles =  '&adressesHistoCiviles=false';
	}
	if (E$(elementIdCivilesConjoint).checked ){
		histoCivilesConjoint = '&adressesHistoCivilesConjoint=true';
	}
	else {
		histoCivilesConjoint =  '&adressesHistoCivilesConjoint=false';
	}
	document.location.href = 'visu.do?id=' + numero + histo + histoCiviles + histoCivilesConjoint;
}

/**
 * 
 */
function togglePanels(elementId, element1, element2) {
	if (E$(elementId).checked){
		Element.hide(element1);
		Element.show(element2);
	}
	else {
		Element.show(element1);
		Element.hide(element2);
	}
}