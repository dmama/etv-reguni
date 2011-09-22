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

	// on récupère les valeurs courantes / par défaut
	var motifOuverture = $('#' + motifsOuvertureSelectId).val();
	if (motifOuverture == null) motifOuverture = defaultMotifOuverture;
	var motifsFermeture = $('#' + motifsFermetureSelectId).val();
	if (motifsFermeture == null) motifsFermeture = defaultMotifFermeture;
	var genreImpot = $('#' + genreImpotSelectId).val();
	var rattachement = $('#' + rattachementSelectId).val();
	
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
 * Cette fonction met-à-jour la liste des motifs d'ouverture en fonction des
 * valeurs de genre d'impôt et motif de rattachement spécifiés.
 * 
 * @param element
 *            l'élément qui vient d'être changé
 * @param motifsOuvertureSelectId
 *            l'id du select contenant les motifs d'ouverture à mettre-à-jour
 * @param genreImpot
 *            le genre d'impôt courant
 * @param rattachement
 *            le motif de rattachement courant
 * @param defaultMotifOuverture
 *            (optionel) la valeur par défaut du motif de ouverture
 *            lorsqu'aucune valeur n'est sélectionné dans la liste
 *            'motifsOuvertureSelectId'
 */
function updateMotifsOuverture(element, motifsOuvertureSelectId, numeroCtb, genreImpot, rattachement, defaultMotifOuverture) {
	
	var motifsOuverture = $('#' + motifsOuvertureSelectId).val();
	if (motifsOuverture == null) motifsOuverture = defaultMotifOuverture;
	 
	// appels ajax pour mettre-à-jour les motifs de ouverture
	XT.doAjaxAction('updateMotifsOuverture', element, {
		'motifsOuvertureSelectId' : motifsOuvertureSelectId,
		'numeroCtb' : numeroCtb,
		'genreImpot' : genreImpot,
		'rattachement' : rattachement,
		'motifCourant' : motifsOuverture
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
	
	var motifsFermeture = $('#' + motifsFermetureSelectId).val();
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
		$('#for_fraction_commune_label').show();
		$('#for_fraction_commune').show();
		$('#for_commune_label').hide();
		$('#for_commune').hide();
		$('#for_pays_label').hide();
		$('#for_pays').hide();
	} else if( name == 'COMMUNE_HC' ){
		$('#for_fraction_commune_label').hide();
		$('#for_fraction_commune').hide();
		$('#for_commune_label').show();
		$('#for_commune').show();
		$('#for_pays_label').hide();
		$('#for_pays').hide();
	} else if( name == 'PAYS_HS' ){
		$('#for_fraction_commune_label').hide();
		$('#for_fraction_commune').hide();
		$('#for_commune_label').hide();
		$('#for_commune').hide();
		$('#for_pays_label').show();
		$('#for_pays').show();
	} 
}

/*
* Selection d'un type de for fiscal pour les for DPI
*/
function selectForFiscalDPI(name) {
	if( name == 'COMMUNE_OU_FRACTION_VD' ){
		$('#for_fraction_commune_label').show();
		$('#for_fraction_commune').show();
		$('#for_commune_label').hide();
		$('#for_commune').hide();
	} else if( name == 'COMMUNE_HC' ){
		$('#for_fraction_commune_label').hide();
		$('#for_fraction_commune').hide();
		$('#for_commune_label').show();
		$('#for_commune').show();
	}
}

/*
* Selection du genre d'impot
*/
function selectGenreImpot(name, callback) {
	var divRattachement = $('#div_rattachement');
	var divRattachementLabel = $('#div_rattachement_label');
	var divDateForPeriodique = $('#date_for_periodique');
	var divMotifForPeriodique = $('#motif_for_periodique');
	var divForUnique = $('#for_unique');
	var divSelectTypeFor = $('#select_type_for');
	var optTypeFor = $('#optionTypeAutoriteFiscale');
	var	divTypeForFraction = $('#type_for_fraction');
	var divTypeForHS = $('#type_for_hs');
	var rattachement = $('#rattachement');
	var	divModeImposition = $('#mode_imposition');
	
	if (name == 'REVENU_FORTUNE'){
		// callback à cause d'un bug IE6 qui raffiche la combo même si elle est cachée 
		if (callback) {
			callback($('#genre_impot').get(0));
		}
		divRattachementLabel.show();
		divRattachement.show();
		divDateForPeriodique.show();
		divMotifForPeriodique.show();
		divForUnique.hide();
		if (rattachement.val() == 'DOMICILE') {
			divSelectTypeFor.show();
			divTypeForFraction.hide();
			divTypeForHS.hide();
			divModeImposition.show();
		} else if (rattachement.val() == 'DIPLOMATE_ETRANGER') {
			//for hors suisse
			divSelectTypeFor.hide();
			optTypeFor.val('PAYS_HS');
			divTypeForFraction.hide();
			divTypeForHS.show();
			$('#for_fraction_commune_label').hide();
			$('#for_fraction_commune').hide();
			$('#for_commune_label').hide();
			$('#for_commune').hide();
			$('#for_pays_label').show();
			$('#for_pays').show();
			divModeImposition.hide();
		} else {
			//for vaudois
			divSelectTypeFor.hide();
			optTypeFor.val('COMMUNE_OU_FRACTION_VD');
			divTypeForFraction.show();
			divTypeForHS.hide();
			$('#for_fraction_commune_label').show();
			$('#for_fraction_commune').show();
			$('#for_commune_label').hide();
			$('#for_commune').hide();
			$('#for_pays_label').hide();
			$('#for_pays').hide();
			divModeImposition.hide();
		}
	} else { 
		//for vaudois
		divRattachementLabel.hide();
		divRattachement.hide();
		divDateForPeriodique.hide();
		divMotifForPeriodique.hide();
		divForUnique.show();
		divSelectTypeFor.hide();
		optTypeFor.val('COMMUNE_OU_FRACTION_VD');
		divTypeForFraction.show();
		divTypeForHS.hide();
		$('#for_fraction_commune_label').show();
		$('#for_fraction_commune').show();
		$('#for_commune_label').hide();
		$('#for_commune').hide();
		$('#for_pays_label').hide();
		$('#for_pays').hide();
		divModeImposition.hide();
	}
}

/*
* Selection du rattachement
*/
function selectRattachement(name) {
	var divSelectTypeFor = $('#select_type_for');
	var optTypeFor = $('#optionTypeAutoriteFiscale');
	var	divTypeForFraction = $('#type_for_fraction');
	var divTypeForHS = $('#type_for_hs');
	var	divModeImposition = $('#mode_imposition');
	
	if (name == 'DOMICILE'){
		divSelectTypeFor.show();
		divTypeForFraction.hide();
		divTypeForHS.hide();
		divModeImposition.show();
	} else if (name == 'DIPLOMATE_ETRANGER') {
		//for hors suisse
		divSelectTypeFor.hide();
		optTypeFor.val('PAYS_HS');
		divTypeForFraction.hide();
		divTypeForHS.show();
		$('#for_fraction_commune_label').hide();
		$('#for_fraction_commune').hide();
		$('#for_commune_label').hide();
		$('#for_commune').hide();
		$('#for_pays_label').show();
		$('#for_pays').show();
		divModeImposition.hide();
	} else {
		//for vaudois
		divSelectTypeFor.hide();
		optTypeFor.val('COMMUNE_OU_FRACTION_VD');
		divTypeForFraction.show();
		divTypeForHS.hide();
		$('#for_fraction_commune_label').show();
		$('#for_fraction_commune').show();
		$('#for_commune_label').hide();
		$('#for_commune').hide();
		$('#for_pays_label').hide();
		$('#for_pays').hide();
		divModeImposition.hide();
	} 
}

/*
* Annuler un for
*/
function annulerFor(idFor) {
	if(confirm('Voulez-vous vraiment annuler ce for fiscal ?')) {
		Form.doPostBack("theForm", "annulerFor", idFor);
 	}
}	
