
/**
 * Classe utilitaire pour la manipulation de données en relation avec les tiers
 */
var Tiers = {
	/**
	 * Récupère des informations générales sur un tiers (voir la classe java TiersInfoController pour le détails des données retournées)
	 */
	queryInfo : function(numero, callback) {
		$.getJSON(getContextPath() + 'tiers/info.do?numero=' + numero, callback);
	}
}


/**
* Efface les valeurs des criteres de recherche du Host
*/
function effacerCriteres() {
  top.location.replace('list.do?action=effacer');
}

/*
* Toggle rows is actif
*/
function toggleRowsIsHisto(tableId,elementId, numCol){
	
	var tbl = $('#' + tableId).get(0);
	if (tbl != null) {
		var len = tbl.rows.length;
		var showHisto = $('#' + elementId).attr('checked');

		for (i=1 ; i< len; i++) {
			if (!showHisto) {
				var x = tbl.rows[i].cells;
				if (numCol >= x.length) {
					// work-around parce que le tag <display:table> ajoute une ligne avec une *seule* colonne lorsque la table est vide
					// cette ligne est masquée par défaut, on ne fait donc rien
				}
				else if (trim(x[numCol].innerHTML) == '' && x[numCol].innerHTML.indexOf('strike')== -1 && !hasClassName(tbl.rows[i], 'strike')) {
					tbl.rows[i].style.display = '';
				}
				else {
					tbl.rows[i].style.display = 'none';
				}
			}
			else {
				tbl.rows[i].style.display = '';
			}
		}
		
		if (tableId == 'dossierApparente') {
			filter(tbl, $('#typeRapportId'));
		}
	}
}	


/*
* Toggle rows is actif
*/
function toggleRowsIsHistoPeriodicite(tableId,elementId, numCol,numColActive){

	var tbl = $('#' + tableId).get(0);
	if (tbl != null) {
		var len = tbl.rows.length;
		var showHisto = $('#' + elementId).attr('checked');

		for (i=1 ; i< len; i++){
			if (!showHisto) {
				var x = tbl.rows[i].cells;
				if ((trim(x[numCol].innerHTML) == '') && (!hasClassName(tbl.rows[i], 'strike'))||(x[numColActive].innerHTML.match('Active'))){
					tbl.rows[i].style.display = '';
				}
				else {
					tbl.rows[i].style.display = 'none';
				}
			}
			else {
				tbl.rows[i].style.display = '';
			}
		}

	}
}
/*
* Toggle rows is actif
*/
function toggleRowsIsActif(tableId, elementId, numCol){

	var tbl = $('#' + tableId).get(0);
	if (tbl != null) {
		var len = tbl.rows.length;
		var showHisto = $('#' + elementId).attr('checked');

		for (i=1 ; i< len; i++){
			if (!showHisto) {
				var x = tbl.rows[i].cells;
				if ((x[numCol].innerHTML.indexOf('strike')== -1) && (!hasClassName(tbl.rows[i], 'strike'))){
					tbl.rows[i].style.display = '';
				}
				else {
					tbl.rows[i].style.display = 'none';
				}
			}
			else {
				tbl.rows[i].style.display = '';
			}
		}
	}
}	

/*
* Filter select box
*/
function filter(tbl, element){
	var sel  = $(element).val();
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
		Form.doPostBack("theForm", "annulerRapport", idRapport);
 	}
}

/*
 * Annuler un rapport entre un débiteur et un sourcier
 */
function annulerRapportPrestation(idRapport) {
	if (confirm('Voulez-vous vraiment annuler ce rapport entre tiers ?')) {
		var form = $('<form method="POST" action="' + getContextPath() + '/rapports-prestation/edit.do">' +
			'<input type="hidden" name="__TARGET__" value="annulerRapport"/>' +
			'<input type="hidden" name="__EVENT_ARGUMENT__" value="' + idRapport + '"/>' +
			'<input type="hidden" name="__URL_RETOUR__" value="' + window.location + '"/></form>');
		form.appendTo('body'); // [UNIREG-3256] obligatoire pour que cela fonctionne avec IE6
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
		$('#div_periodeDecompte_label').show();
		$('#div_periodeDecompte_input').show();
	} else {
		$('#div_periodeDecompte_label').hide();
		$('#div_periodeDecompte_input').hide();
	} 
}

/*
* Selection d'un logiciel
*/
function selectLogiciel(name) {
	if( name == 'ELECTRONIQUE' ){
		$('#div_logiciel_label').show();
		$('#div_logiciel_input').show();
	} else {
		$('#div_logiciel_label').hide();
		$('#div_logiciel_input').hide();
	}
}

/**
* Affichage des adresses actives ou pas
*/ 
function afficheAdressesHisto(elementId,elementIdCiviles,elementIdCivilesConjoint, numero) {
	var histo;
	var histoCiviles;
	var histoCivilesConjoint;

	if ($('#' + elementId).attr('checked')) {
		histo = '&adressesHisto=true';
	}
	else {
		histo =  '&adressesHisto=false';
	}

	if ($(elementIdCiviles).attr('checked')) {
		histoCiviles = '&adressesHistoCiviles=true';
	}
	else {
		histoCiviles =  '&adressesHistoCiviles=false';
	}
	if ($(elementIdCivilesConjoint).attr('checked')) {
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
	if ($('#' + elementId).attr('checked')){
		$('#' + element1).hide();
		$('#' + element2).show();
	}
	else {
		$('#' + element1).show();
		$('#' + element2).hide();
	}
}

	// ----------------------------------------------------------------------------
// HasClassName
//
// Description : returns boolean indicating whether the object has the class name
//    built with the understanding that there may be multiple classes
//
// Arguments:
//    objElement              - element to manipulate
//    strClass                - class name to find
//
function hasClassName(objElement, strClass)
   {

   // if there is a class
   if ( objElement.className )
      {

      // the classes are just a space separated list, so first get the list
      var arrList = objElement.className.split(' ');

      // get uppercase class for comparison purposes
      var strClassUpper = strClass.toUpperCase();

      // find all instances and remove them
      for ( var i = 0; i < arrList.length; i++ )
         {

         // if class found
         if ( arrList[i].toUpperCase() == strClassUpper )
            {

            // we found it
            return true;

            }

         }

      }

   // if we got here then the class name is not there
   return false;

   }
//
// HasClassName
// ----------------------------------------------------------------------------

/**
 * Affiche ou filtre les données historiques d'une table
 */
function refreshHistoTable(showHisto, table, dateFinIndex) {
	var len = $(table).get(0).rows.length;
	var firstLine = null;
	var foundSomething = false; // vrai si une ligne au moins est affichée
	var visibleCount = 0;

	for (i = 1; i < len; i++) { // on ignore l'entête
		var line = table.rows[i];
		if (i == 1) {
			firstLine = line;
		}
		var dateFin = line.cells[dateFinIndex].innerHTML;
		var isHisto = (dateFin != null && isNotBlank(dateFin)); // date fin != null -> valeur historique

		// affiche ou cache la ligne
		if (isHisto) {
			if (showHisto) {
				line.style.display = '';
			}
			else {
				line.style.display = 'none';
			}
		}
		else {
			foundSomething = true;
		}

		if (showHisto || !isHisto) {
			// on adapte le style des lignes odd/even
			line.className = (visibleCount++ % 2 == 0 ? 'even' : 'odd');
		}
	}
	if (!showHisto && !foundSomething) { // si toutes les valeurs sont historiques, on affiche au minimum la plus récente
		firstLine.style.display = ''
	}
}

function trim(string) {
	if (string.trim) {
		return string.trim();
	}
	else {
		return string.replace(/^\s+|\s+$/g, "");
	}
};

function isBlank(s) {
	return trim(s).length == 0;
};

function isNotBlank(s) {
	return !isBlank(s);
};

function annulerAdresse(idAdresse) {
	if (confirm('Voulez-vous vraiment annuler cette adresse surchargée ?')) {
		var form = $('<form method="POST" action="' + getContextPath() + '/adresses/edit.do">' +
			'<input type="hidden" name="__TARGET__" value="annulerAdresse"/>' +
			'<input type="hidden" name="__EVENT_ARGUMENT__" value="' + idAdresse + '"/></form>');
		form.appendTo('body'); // [UNIREG-3151] obligatoire pour que cela fonctionne avec IE6
		form.submit();
 	}
}
