/**
 * Cette fonction met-à-jour la liste des motifs d'ouverture et de fermeture en
 * fonction des valeurs de genre d'impôt et motif de rattachement spécifiés.
 * 
 * @param element
 *            l'élément qui vient d'être changé
 * @param motifsOuvertureSelectId
 *            l'id du select contenant les motifs d'ouverture à mettre-à-jour
 * @param motifsFermetureSelectId
 *            l'id du select contenant les motifs de fermeture à mettre-à-jour
 * @param genreImpotSelectId
 *            l'id du select contenant le genre d'impôt courant
 * @param rattachementSelectId
 *            l'id du select contenant le motif de rattachement courant
 * @param defaultMotifOuverture
 *            (optionel) la valeur par défaut du motif d'ouverture lorsqu'aucune
 *            valeur n'est sélectionné dans la liste 'motifsOuvertureSelectId'
 * @param defaultMotifFermeture
 *            (optionel) la valeur par défaut du motif de fermeture
 *            lorsqu'aucune valeur n'est sélectionné dans la liste
 *            'motifsFermetureSelectId'
 */
function updateMotifsFor(element, motifsOuvertureSelectId, motifsFermetureSelectId, numeroCtb, genreImpotSelectId, rattachementSelectId, defaultMotifOuverture, defaultMotifFermeture) {

	var motifsOuvertureSelect = E$(motifsOuvertureSelectId);
	var motifsFermetureSelect = E$(motifsFermetureSelectId);
	var genreImpotSelect = E$(genreImpotSelectId);
	var rattachementSelect = E$(rattachementSelectId);

	// on récupère les valeurs courantes / par défaut
	var motifOuverture = (motifsOuvertureSelect.selectedIndex < 0 ? null : motifsOuvertureSelect.options[motifsOuvertureSelect.selectedIndex].value);
	if (motifOuverture == null) motifOuverture = defaultMotifOuverture;
	var motifsFermeture = (motifsFermetureSelect.selectedIndex < 0 ? null : motifsFermetureSelect.options[motifsFermetureSelect.selectedIndex].value);
	if (motifsFermeture == null) motifsFermeture = defaultMotifFermeture;
	var genreImpot = (genreImpotSelect == null ? null : genreImpotSelect.options[genreImpotSelect.selectedIndex].value);
	var rattachement = (rattachementSelect == null ? null : rattachementSelect.options[rattachementSelect.selectedIndex].value);
	
	// appels ajax pour mettre-à-jour les motifs d'ouverture
	XT.doAjaxAction('updateMotifsOuverture', element, {
		'motifsOuvertureSelectId' : motifsOuvertureSelectId,
		'numeroCtb' : numeroCtb,
		'genreImpot' : genreImpot,
		'rattachement' : rattachement,
		'motifCourant' : motifOuverture
	});

	// appels ajax pour mettre-à-jour les motifs de fermeture
	XT.doAjaxAction('updateMotifsFermeture', element, {
		'motifsFermetureSelectId' : motifsFermetureSelectId,
		'numeroCtb' : numeroCtb,
		'genreImpot' : genreImpot,
		'rattachement' : rattachement,
		'motifCourant' : motifsFermeture
	});
}

/**
 * Cette fonction met-à-jour la liste des motifs de fermeture en fonction des
 * valeurs de genre d'impôt et motif de rattachement spécifiés.
 * 
 * @param element
 *            l'élément qui vient d'être changé
 * @param motifsFermetureSelectId
 *            l'id du select contenant les motifs de fermeture à mettre-à-jour
 * @param genreImpot
 *            le genre d'impôt courant
 * @param rattachement
 *            le motif de rattachement courant
 * @param defaultMotifFermeture
 *            (optionel) la valeur par défaut du motif de fermeture
 *            lorsqu'aucune valeur n'est sélectionné dans la liste
 *            'motifsFermetureSelectId'
 */
function updateMotifsFermeture(element, motifsFermetureSelectId, numeroCtb, genreImpot, rattachement, defaultMotifFermeture) {
	
	var motifsFermetureSelect = E$(motifsFermetureSelectId);
	var motifsFermeture = (motifsFermetureSelect.selectedIndex < 0 ? null : motifsFermetureSelect.options[motifsFermetureSelect.selectedIndex].value);
	if (motifsFermeture == null) motifsFermeture = defaultMotifFermeture;
	 
	// appels ajax pour mettre-à-jour les motifs de fermeture
	XT.doAjaxAction('updateMotifsFermeture', element, {
		'motifsFermetureSelectId' : motifsFermetureSelectId,
		'numeroCtb' : numeroCtb,
		'genreImpot' : genreImpot,
		'rattachement' : rattachement,
		'motifCourant' : motifsFermeture
	});
}

/*
* Selection d'un type de for fiscal
*/
function selectForFiscal(name) {
	if( name == 'COMMUNE_OU_FRACTION_VD' ){
		Element.show('for_fraction_commune_label');
		Element.show('for_fraction_commune');
		Element.hide('for_commune_label');
		Element.hide('for_commune');
		Element.hide('for_pays_label');
		Element.hide('for_pays');
	} else if( name == 'COMMUNE_HC' ){
		Element.hide('for_fraction_commune_label');
		Element.hide('for_fraction_commune');
		Element.show('for_commune_label');
		Element.show('for_commune');
		Element.hide('for_pays_label');
		Element.hide('for_pays');
	} else if( name == 'PAYS_HS' ){
		Element.hide('for_fraction_commune_label');
		Element.hide('for_fraction_commune');
		Element.hide('for_commune_label');
		Element.hide('for_commune');
		Element.show('for_pays_label');
		Element.show('for_pays');
	} 
}

/*
* Selection d'un type de for fiscal pour les for DPI
*/
function selectForFiscalDPI(name) {
	if( name == 'COMMUNE_OU_FRACTION_VD' ){
		Element.show('for_fraction_commune_label');
		Element.show('for_fraction_commune');
		Element.hide('for_commune_label');
		Element.hide('for_commune');
	} else if( name == 'COMMUNE_HC' ){
		Element.hide('for_fraction_commune_label');
		Element.hide('for_fraction_commune');
		Element.show('for_commune_label');
		Element.show('for_commune');
	}
}

/*
* Selection du genre d'impot
*/
function selectGenreImpot(name, callback) {
	var divRattachement = E$('div_rattachement');
	var divRattachementLabel = E$('div_rattachement_label');
	var divDateForPeriodique = E$('date_for_periodique');
	var divMotifForPeriodique = E$('motif_for_periodique');
	var divForUnique = E$('for_unique');
	var divSelectTypeFor = E$('select_type_for');
	var optTypeFor = E$('optionTypeAutoriteFiscale');
	var	divTypeForFraction = E$('type_for_fraction');
	var divTypeForHS = E$('type_for_hs');
	var rattachement = E$('rattachement');
	var	divModeImposition = E$('mode_imposition');
	
	if (name == 'REVENU_FORTUNE'){
		// callback à cause d'un bug IE6 qui raffiche la combo même si elle est cachée 
		if (callback) {
			callback(E$('genre_impot'));
		}
		divRattachementLabel.style.display = '';
		divRattachement.style.display = '';
		divDateForPeriodique.style.display = '';
		divMotifForPeriodique.style.display = '';
		divForUnique.style.display = 'none';
		if (rattachement.value == 'DOMICILE') {
			divSelectTypeFor.style.display = '';
			divTypeForFraction.style.display = 'none';
			divTypeForHS.style.display = 'none';
			divModeImposition.style.display = '';
		} else if (rattachement.value == 'DIPLOMATE_ETRANGER') {
			//for hors suisse
			divSelectTypeFor.style.display = 'none';
			optTypeFor.value = 'PAYS_HS';
			divTypeForFraction.style.display = 'none';
			divTypeForHS.style.display = '';
			Element.hide('for_fraction_commune_label');
			Element.hide('for_fraction_commune');
			Element.hide('for_commune_label');
			Element.hide('for_commune');
			Element.show('for_pays_label');
			Element.show('for_pays');
			divModeImposition.style.display = 'none';
		} else {
			//for vaudois
			divSelectTypeFor.style.display = 'none';
			optTypeFor.value = 'COMMUNE_OU_FRACTION_VD';
			divTypeForFraction.style.display = '';
			divTypeForHS.style.display = 'none';
			Element.show('for_fraction_commune_label');
			Element.show('for_fraction_commune');
			Element.hide('for_commune_label');
			Element.hide('for_commune');
			Element.hide('for_pays_label');
			Element.hide('for_pays');
			divModeImposition.style.display = 'none';
		}
	} else { 
		//for vaudois
		divRattachementLabel.style.display = 'none';
		divRattachement.style.display = 'none';
		divDateForPeriodique.style.display = 'none';
		divMotifForPeriodique.style.display = 'none';
		divForUnique.style.display = '';
		divSelectTypeFor.style.display = 'none';
		optTypeFor.value = 'COMMUNE_OU_FRACTION_VD';
		divTypeForFraction.style.display = '';
		divTypeForHS.style.display = 'none';
		Element.show('for_fraction_commune_label');
		Element.show('for_fraction_commune');
		Element.hide('for_commune_label');
		Element.hide('for_commune');
		Element.hide('for_pays_label');
		Element.hide('for_pays');
		divModeImposition.style.display = 'none';
	}
}

/*
* Selection du rattachement
*/
function selectRattachement(name) {
	var divSelectTypeFor = E$('select_type_for');
	var optTypeFor = E$('optionTypeAutoriteFiscale');
	var	divTypeForFraction = E$('type_for_fraction');
	var divTypeForHS = E$('type_for_hs');
	var	divModeImposition = E$('mode_imposition');
	
	if (name == 'DOMICILE'){
		divSelectTypeFor.style.display = '';
		divTypeForFraction.style.display = 'none';
		divTypeForHS.style.display = 'none';
		divModeImposition.style.display = '';
	} else if (name == 'DIPLOMATE_ETRANGER') {
		//for hors suisse
		divSelectTypeFor.style.display = 'none';
		optTypeFor.value = 'PAYS_HS';
		divTypeForFraction.style.display = 'none';
		divTypeForHS.style.display = '';
		Element.hide('for_fraction_commune_label');
		Element.hide('for_fraction_commune');
		Element.hide('for_commune_label');
		Element.hide('for_commune');
		Element.show('for_pays_label');
		Element.show('for_pays');
		divModeImposition.style.display = 'none';
	} else {
		//for vaudois
		divSelectTypeFor.style.display = 'none';
		optTypeFor.value = 'COMMUNE_OU_FRACTION_VD';
		divTypeForFraction.style.display = '';
		divTypeForHS.style.display = 'none';
		Element.show('for_fraction_commune_label');
		Element.show('for_fraction_commune');
		Element.hide('for_commune_label');
		Element.hide('for_commune');
		Element.hide('for_pays_label');
		Element.hide('for_pays');
		divModeImposition.style.display = 'none';
	} 
}

/*
* Annuler un for
*/
function annulerFor(idFor) {
	if(confirm('Voulez-vous vraiment annuler ce for fiscal ?')) {
		var form = F$("theForm");
		form.doPostBack("annulerFor", idFor);
 	}
}	
