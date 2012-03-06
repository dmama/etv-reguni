
/**
 * Classe utilitaire pour la manipulation de données en relation avec les tiers
 */
var Tiers = {
	/**
	 * Récupère des informations générales sur un tiers (voir la classe java TiersInfoController pour le détails des données retournées)
	 */
	queryInfo : function(numero, callback) {
		$.getJSON(getContextPath() + 'tiers/info.do?numero=' + numero + '&' + new Date().getTime(), callback);
	},

	/**
	 * Formatte un numéro de tiers pour l'affichage.
	 *
	 * Exemple : 54, 1.05, 123.34, 21.764.00, 120.223.344.
	 */
	formatNumero: function(numero) {
		var s = '';
		if (numero) {
			var numero = '' + numero;
			var length = numero.length;
			if (length < 3) {
				s = numero;
			}
			else if (length < 6) {
				s = numero.substring(0, length - 2) + '.' + numero.substring(length - 2);
			}
			else {
				s = numero.substring(0, length - 5) + '.' + numero.substring(length - 5, length - 2) + '.' + numero.substring(length - 2);
			}
		}
		return s;
	},

	linkTo: function(numero) {
		var s = '';
		if (numero) {
			s = '<a href="' + getContextPath() + 'tiers/visu.do?id=' + numero + '">' + this.formatNumero(numero) + '</a>';
		}
		return s;
	}
}


/**
* Efface les valeurs des criteres de recherche du Host
*/
function effacerCriteres() {
  top.location.replace('list.do?action=effacer');
}

/**
 * Affiche ou cache les lignes qui possèdent une date de fin.
 *
 * @tableId   l'id de la table
 * @elementId l'id du checkbox d'affichage de l'historique
 * @numCol    le numéro de la colonne (0-based) qui contient les dates de fin
 * @cond      condition optionnelle de visibilité, appelée sur chaque ligne (true=visible selon algo, false=toujours invisible)
 */
function toggleRowsIsHisto(tableId, elementId, numCol, cond) {
	
	var tbl = $('#' + tableId).get(0);
	if (tbl != null) {
		var len = tbl.rows.length;
		var showHisto = $('#' + elementId).attr('checked');

		for (i = 1 ; i < len; i++) {

			var visible;
			if (!showHisto) {
				var x = tbl.rows[i].cells;
				if (numCol >= x.length) {
					// work-around parce que le tag <display:table> ajoute une ligne avec une *seule* colonne lorsque la table est vide
					// cette ligne est masquée par défaut, on ne fait donc rien
					continue;
				}
				else if (trim(x[numCol].innerHTML) == '' && x[numCol].innerHTML.indexOf('strike')== -1 && !hasClassName(tbl.rows[i], 'strike')) {
					visible = true;
				}
				else {
					visible = false;
				}
			}
			else {
				visible = true;
			}

			if (visible && cond) {
				visible = cond(tbl.rows[i]);
			}

			tbl.rows[i].style.display = (visible ? '' : 'none');
		}
	}
}	


/*
* Affichage des trois premières lignes d'un tableau ou de toutes les lignes en fonction du choix de l'utilisateur
*/
function toggleAffichageRows(tableId, isAll, numCol) {

	var tbl = $('#' + tableId).get(0);
	if (tbl != null) {
		var len = tbl.rows.length;
		for (i=1 ; i< len; i++) {
			if (!isAll) {
				var x = tbl.rows[i].cells;
				if (numCol >= x.length) {
					// work-around parce que le tag <display:table> ajoute une ligne avec une *seule* colonne lorsque la table est vide
					// cette ligne est masquée par défaut, on ne fait donc rien
				}
				else if (i <= 3) {
					tbl.rows[i].style.display = '';
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
	}

	//Affichage et masquage des liens

	var showall = $('#linkAll');
	var showReduce = $('#linkReduce');

	if (isAll){
		showall.hide();
		showReduce.show();
	}
	else {
		showall.show();
		showReduce.hide();
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
    var rows = $(table).get(0).rows;
	var foundSomething = false; // vrai si une ligne au moins est affichée
	var visibleCount = 0;

	for (i = 1; i < rows.length; i++) { // on ignore l'entête
		var line = rows[i];
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
	if (!showHisto && !foundSomething && rows.length > 1) { // si toutes les valeurs sont historiques, on affiche au minimum la plus récente
		rows[1].style.display = ''
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
