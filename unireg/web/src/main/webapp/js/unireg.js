/**
 * Classe utilitaire pour la manipulation de données en relation avec les tiers
 */
 var Autocomplete = {

	/**
	 * Méthode pour activer l'autocompletion sur un champs de saisie texte. Les données utilisées pour l'autocompletion sont extraites du service d'infrastructure.
	 *
	 * @param category    		la catégorie de données utilisées comme autocompletion. Voir la classe java AutoCompleteInfraController pour les catégories supportées.
	 * @param input     		le champ de saisie texte (ou son id) sur lequel l'autocompletion sera effective
	 * @param validateSelection	active le validation de la selection, qui devient rouge si le texte saisi ne correspond à aucune valeur connue ([SIFISC-832])
	 * @param on_change(item)   une function callback (optionnelle) appelée lorsqu'une valeur a été choisie par l'utilisateur
	 */
	infra: function(category, input, validateSelection, on_change) {
		this.generic("/autocomplete/infra.do?category=" + category, input, validateSelection, on_change);
	},

	/**
	 * Méthode pour activer l'autocompletion sur un champs de saisie texte. Les données utilisées pour l'autocompletion sont extraites du service de sécurité.
	 *
	 * @param category    		la catégorie de données utilisées comme autocompletion. Voir la classe java AutoCompleteSecurityController pour les catégories supportées.
	 * @param input     		le champ de saisie texte (ou son id) sur lequel l'autocompletion sera effective
	 * @param validateSelection	active le validation de la selection, qui devient rouge si le texte saisi ne correspond à aucune valeur connue ([SIFISC-832])
	 * @param on_change(item)   une function callback (optionnelle) appelée lorsqu'une valeur a été choisie par l'utilisateur
	 */
	security: function(category, input, validateSelection, on_change) {
		this.generic("/autocomplete/security.do?category=" + category, input, validateSelection, on_change);
	},

	/**
	 * Méthode pour activer l'autocompletion sur un champs de saisie texte.
	 *
	 * @param url               l'url d'accès à la source des données d'autocompletion,
	 * @param input     		le champ de saisie texte (ou son id) sur lequel l'autocompletion sera effective
	 * @param validateSelection	active le validation de la selection, qui devient rouge si le texte saisi ne correspond à aucune valeur connue ([SIFISC-832])
	 * @param on_change(item)   une function callback (optionnelle) appelée lorsqu'une valeur a été choisie par l'utilisateur
	 */
	generic: function(url, input, validateSelection, on_change) {

		var input = $(input);

		// [SIFISC-832] on évite de passer par des variables locales parce qu'on cas de réinstanciation de l'autocompletion sur un champ,
		// cela crée plusieurs closures avec chacunes leurs variables locales. Et après, il y a plusieurs versions de l'event handler sur 'focusout'
		// et seulement une seule qui voit le bon item sélectionné.
		//var is_open = false;
		//var selected = null;

		input.autocomplete({
			source: getContextPath() + url,
			open: function(event, ui) {
				if (validateSelection) {
					input.removeClass('error');
				}
				input.data('selected', null);
				input.data('is_open', true);
			},
			focus: function(event, ui) {
				// on met-à-jour la valeur affichée
				input.val(ui.item.label);
				return false;
			},
			select: function(event, ui) {
				// on mémorise la valeur sélectionnée
				input.data('selected', ui.item);
				return false;
			},
			close: function(event, ui) {
				input.data('is_open', false);
				if (validateSelection && input.data('selected') == null) {
					input.addClass('error');
				}
				// à la fermeture de l'auto-complete, on notifie de la valeur sélectionnée
				if (on_change) {
					on_change(input.data('selected'));
				}
			},
			minLength: 2
		})
		.data( "autocomplete" )._renderItem = function( ul, item ) {
			return $( "<li></li>" )
				.data( "item.autocomplete", item )
				.append( "<a>" + item.desc + "</a>" )
				.appendTo( ul );
		};

		input.focusout(function(event) {
			if (input.data('is_open')) {
				// on ignore l'événement si le dialog d'autocompletion est ouverte, parce que cela veut dire que l'utilisateur a cliqué avec la souris sur un élément du menu
				return;
			}
			// si l'utilisateur a modifié le champ sans que l'autocompletion s'active, on force la notification en fin d'édition
			if (validateSelection && input.data('selected') == null) {
				input.addClass('error');
			}
			if (on_change) {
				on_change(input.data('selected'));
			}
		});

		var previous = null;
		input.keyup(function(event) {
			var current = input.val();
			if (current != previous) {
				previous = current;
				// on met-à-null la valeur chaque fois que le texte de saisie change
				input.data('selected', null);
			}
		});
	}
}

var Dialog = {

	/**
	 * Ouvre une boîte de dialogue modale qui permet de rechercher et de sélectionner un tiers.
	 * <p>
	 * Exemple d'utilisation:
	 * <pre>
	 *     <button onclick="return Dialog.open_tiers_picker(this, function(id) {alert('le tiers n°' + id + ' a été sélectionné');});">...</button>
	 * </pre>
	 *
	 * @param button              le button html sur lequel l'utilisateur a cliqué
	 * @param on_tiers_selection  fonction de callback appelée avec le numéro de tiers sélectionné par l'utilisateur
	 */
	open_tiers_picker: function(button, on_tiers_selection) {
		return Dialog.open_tiers_picker_with_filter(button, null, null, on_tiers_selection);
	},

	/**
	 * Ouvre une boîte de dialogue modale qui permet de rechercher et de sélectionner un tiers, le tout avec un filtre sur les résultats.
	 * <p>
	 * Exemple d'utilisation:
	 * <pre>
	 *     <button onclick="return Dialog.open_tiers_picker_with_filter(this, 'tiersPickerFilterFactory', 'typeTiers:PERSONNE_PHYSIQUE', function(id) {alert('le tiers n°' + id + ' a été sélectionné');});">...</button>
	 * </pre>
	 *
	 * @param button              le button html sur lequel l'utilisateur a cliqué
	 * @param filter_bean		  le nom d'un bean spring qui implément l'interface TiersPickerFilterFactory
	 * @param filter_params		  les paramètres du filter spécifié dans <i>filter_bean</i>
	 * @param on_tiers_selection  fonction de callback appelée avec le numéro de tiers sélectionné par l'utilisateur
	 */
	open_tiers_picker_with_filter: function(button, filter_bean, filter_params, on_tiers_selection) {

		// on récupère ou on crée à la demande le div de la boîte de dialogue
		var dialog = Dialog.create_dialog_div('tiers-picker-dialog');

		// on définit la méthode appelée lorsque un tiers a été choisi
		$(button).get(0).select_tiers_id = function(link) {
			var tiersId = link.innerHTML.replace(/[^0-9]*/g, ''); // remove non-numeric chars
			dialog.dialog("close");
			if (on_tiers_selection) {
				on_tiers_selection(tiersId);
			}
		};

		// load remote content
		var url = getContextPath() + "/tiers/picker/tiers-picker.do";
		dialog.load(url, function() {
			// Note: le code de cette fonction réfère à des éléments du DOM qui sont dans la boîte de dialogue. En dehors de ce call-back, cela ne fonctionne pas.
			var query = $('#tiers-picker-query');
			var last = "";

			// on installe la fonction qui sera appelée à chaque frappe de touche pour la recherche simple
			query.keyup(function() {
				var current = query.val();
				if (last != current) { // on ne rafraîchit que si le texte a vraiment changé
					last = current;

					clearTimeout($.data(this, "tiers-picker-timer"));
					// on retarde l'appel javascript de 200ms pour éviter de faire plusieurs requêtes lorsque l'utilisateur entre plusieurs caractères rapidemment
					var timer = setTimeout(function() {
						var params = {
							'query' : current,
							'buttonId' : $(button).attr('id')
						};
						if (filter_bean) {
							params['filterBean'] = filter_bean;
							params['filterParams'] = filter_params;
						}
						XT.doAjaxAction('tiersPickerQuickSearch', $('#tiers-picker-query').get(0), params);
					}, 200); // 200 ms
					$.data(this, "tiers-picker-timer", timer);
				}
			});

			// on installe la fonction qui sera appelée lors de la demande de recherche avancée
			var fullSearch = $('#fullSearch').button();
			fullSearch.click(function() {
				var params = {
					'id' : $('#tiers-picker-id').val(),
					'nomraison' : $('#tiers-picker-nomraison').val(),
					'localite' : $('#tiers-picker-localite').val(),
					'datenaissance' : $('#tiers-picker-datenaissance').val(),
					'noavs' : $('#tiers-picker-noavs').val(),
					'buttonId' : $(button).attr('id')
				};
				if (filter_bean) {
					params['filterBean'] = filter_bean;
					params['filterParams'] = filter_params;
				}
				XT.doAjaxAction('tiersPickerFullSearch', $('#tiers-picker-query').get(0), params);
			});

			// la fonction pour tout effacer, y compris les résultat de la recherche
			var fullSearch = $('#clearAll').button();
			fullSearch.click(function() {
				$('#tiers-picker-id').val(null);
				$('#tiers-picker-nomraison').val(null);
				$('#tiers-picker-localite').val(null);
				$('#tiers-picker-datenaissance').val(null);
				$('#tiers-picker-noavs').val(null);
				$('#tiers-picker-results').attr('innerHTML', '');
			});
		});

		// ouvre la boîte de dialogue
		dialog.dialog({
			height: 300,
			width: 800,
			title: "Recherche de tiers",
			modal: true,
			buttons: {
				Annuler: function() {
					dialog.dialog("close");
				}
			}
		});

		//prevent the browser to follow the link
		return false;
	},

	/**
	 * Ouvre une boîte de dialog modale qui affiches les dates de création/modification de l'élément spécifié.
	 *
	 * @param nature le type d'élément
	 * @param id     l'id de l'élément
	 */
	open_consulter_log: function(nature, id) {

		var dialog = Dialog.create_dialog_div('consulter-log-dialog');

		// charge le contenu de la boîte de dialogue
		dialog.load(getContextPath() + '/common/consult-log.do?nature=' + nature + '&id=' + id);

		dialog.dialog({
			title: 'Consultation des logs',
			height: 200,
			width: 800,
			modal: true,
			buttons: {
				Ok: function() {
					dialog.dialog("close");
				}
			}
		});

		//prevent the browser to follow the link
		return false;
	},

	 /**
	  * Ouvre une boîte de dialog modale qui affiches la date et l'utilisateur de traitement d'un message d'identification.
	  *
	  * @param stringDate la date de traitement
	  * @param userTraitement le nom du user de traitement
	  */
	 open_consulter_info_traitement: function(userTraitement, stringDate, stringMessageRetour) {

		var content = "<div style=\"display:none\">" +
					"<table>" +
					"<tr class=\"<unireg:nextRowClass/>\" >" +
					"<td width=\"25%\">Utilisateur de traitement &nbsp;:</td>" +
					"<td width=\"25%\">" + userTraitement + "</td>" +
					"</tr>" +
					"<tr class=\"<unireg:nextRowClass/>\" >" +
					"<td width=\"25%\">Date de traitement&nbsp;:</td>" +
					"<td width=\"25%\">" + stringDate + "</td>" +
					"</tr>" +
					"<tr class=\"<unireg:nextRowClass/>\" >" +
					"<td width=\"25%\">Message de retour&nbsp;:</td>" +
					"<td width=\"25%\">" + stringMessageRetour + "</td>" +
					"</tr>" +
					"</table></div>";

		var dialog = $('#idinfotraitement');
		if (!dialog.length) {
			dialog = $(content);
			dialog.appendTo('body');
		}
		else {
			dialog.attr('innerHTML', content); // on remplace le contenu de la boîte de dialogue
		}

		dialog.dialog({
			title: 'Consultation des informations de traitement',
			height: 160,
			width: 600,
			modal: true,
			buttons: {
				Ok: function() {
					dialog.dialog("close");
				}
			}
		});

		//prevent the browser to follow the link
		return false;
	 },


	/**
	 * Ouvre une boîte de dialog modale qui affiche les détails d'un mouvement de dossier
	 *
	 * @param id l'id du mouvement de dossier
	 */
	open_details_mouvement: function(id) {

		var dialog = Dialog.create_dialog_div('details-mouvement-dialog');

		// charge le contenu de la boîte de dialogue
		dialog.load(getContextPath() + '/tiers/mouvement.do?idMvt=' + id);

		dialog.dialog({
			title: 'Détails du mouvement de dossier',
			height: 440,
			width: 900,
			modal: true,
			buttons: {
				Ok: function() {
					dialog.dialog("close");
				}
			}
		});

		//prevent the browser to follow the link
		return false;
	},

	/**
	 * Récupère ou crée à la demande un élément div pour contenir une boîte de dialogue
	 */
	create_dialog_div: function(id) {
		var dialog = $('#' + id);
		if (!dialog.length) {
			dialog = $('<div id="' + id + '" style="display:hidden"><img src="'+ getContextPath() + '/images/loading.gif"/></div>');
			dialog.appendTo('body');
		}
		else {
			dialog.attr('innerHTML', '<img src="'+ getContextPath() + '/images/loading.gif"/>'); // on vide la boîte de dialogue de son contenu précédant
		}
		return dialog;
	}

}

var Fors = {

	/**
	 * Cette fonction met-à-jour la liste des motifs d'ouverture et de fermeture en
	 * fonction des valeurs de genre d'impôt et motif de rattachement spécifiés.
	 *
	 * @param motifsOuvertureSelect
	 *            le select contenant les motifs d'ouverture à mettre-à-jour
	 * @param motifsFermetureSelect
	 *            le select contenant les motifs de fermeture à mettre-à-jour
	 * @param genreImpotSelectId
	 *            l'id du select contenant le genre d'impôt courant
	 * @param rattachementSelectId
	 *            l'id du select contenant le motif de rattachement courant
	 * @param defaultMotifOuverture
	 *            (optionel) la valeur par défaut du motif d'ouverture lorsqu'aucune
	 *            valeur n'est sélectionné dans la liste 'motifsOuvertureSelect'
	 * @param defaultMotifFermeture
	 *            (optionel) la valeur par défaut du motif de fermeture
	 *            lorsqu'aucune valeur n'est sélectionné dans la liste
	 *            'motifsFermetureSelect'
	 */
	updateMotifsFor: function(motifsOuvertureSelect, motifsFermetureSelect, numeroCtb, genreImpotSelectId, rattachementSelectId, defaultMotifOuverture, defaultMotifFermeture) {

		var genreImpot = $('#' + genreImpotSelectId).val();
		var rattachement = $('#' + rattachementSelectId).val();

		this.updateMotifsOuverture(motifsOuvertureSelect, numeroCtb, genreImpot, rattachement, defaultMotifOuverture);
		this.updateMotifsFermeture(motifsFermetureSelect, numeroCtb, genreImpot, rattachement, defaultMotifFermeture)
	},

	/**
	 * Cette fonction met-à-jour la liste des motifs d'ouverture en fonction des
	 * valeurs de genre d'impôt et motif de rattachement spécifiés.
	 *
	 * @param motifsOuvertureSelect
	 *            le select contenant les motifs d'ouverture à mettre-à-jour
	 * @param genreImpot
	 *            le genre d'impôt courant
	 * @param rattachement
	 *            le motif de rattachement courant
	 * @param defaultMotifOuverture
	 *            (optionel) la valeur par défaut du motif de ouverture
	 *            lorsqu'aucune valeur n'est sélectionné dans la liste
	 *            'motifsOuvertureSelect'
	 */
	updateMotifsOuverture: function(motifsOuvertureSelect, numeroCtb, genreImpot, rattachement, defaultMotifOuverture) {

		var motifOuverture = motifsOuvertureSelect.val();
		if (motifOuverture == null) motifOuverture = defaultMotifOuverture;

		// appels ajax pour mettre-à-jour les motifs d'ouverture
		$.get(getContextPath() + '/fors/motifsOuverture.do?tiersId=' + numeroCtb + '&genreImpot=' + genreImpot + '&rattachement=' + rattachement, function(motifs) {
			var list = '';
			if (!motifOuverture || !(motifOuverture in motifs) && motifs.length > 1) {
				// on ajoute une option vide si le motif courant (= ancienne valeur) n'est pas mappable sur les nouveaux motifs disponibles (et qu'il y en a plusieurs)
				list += '<option></option>';
			}
			for(var i = 0; i < motifs.length; ++i) {
				var motif = motifs[i];
				list += '<option value="' + motif.type + '"' + (motif.type == motifOuverture ? ' selected=true' : '') + '>' + StringUtils.escapeHTML(motif.label) + '</option>';
			}
			motifsOuvertureSelect.html(list);
		});
	},

	/**
	 * Cette fonction met-à-jour la liste des motifs de fermeture en fonction des
	 * valeurs de genre d'impôt et motif de rattachement spécifiés.
	 *
	 * @param motifsFermetureSelect
	 *            le select contenant les motifs de fermeture à mettre-à-jour
	 * @param genreImpot
	 *            le genre d'impôt courant
	 * @param rattachement
	 *            le motif de rattachement courant
	 * @param defaultMotifFermeture
	 *            (optionel) la valeur par défaut du motif de fermeture
	 *            lorsqu'aucune valeur n'est sélectionné dans la liste
	 *            'motifsFermetureSelect'
	 */
	updateMotifsFermeture: function(motifsFermetureSelect, numeroCtb, genreImpot, rattachement, defaultMotifFermeture) {

		var motifFermeture = motifsFermetureSelect.val();
		if (motifFermeture == null) motifFermeture = defaultMotifFermeture;

		// appels ajax pour mettre-à-jour les motifs de fermeture
		$.get(getContextPath() + '/fors/motifsFermeture.do?tiersId=' + numeroCtb + '&genreImpot=' + genreImpot + '&rattachement=' + rattachement, function(motifs) {
			var list = '<option></option>'; // dans le cas du motif de fermeture, on ajoute toujours une option vide
			for(var i = 0; i < motifs.length; ++i) {
				var motif = motifs[i];
				list += '<option value="' + motif.type + '"' + (motif.type == motifFermeture ? ' selected=true' : '') + '>' + StringUtils.escapeHTML(motif.label) + '</option>';
			}
			motifsFermetureSelect.html(list);
		});
	},

	/*
	* Selection d'un type de for fiscal
	*/
	selectForFiscal: function(name) {
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
	},

	/*
	* Selection d'un type de for fiscal pour les for DPI
	*/
	selectForFiscalDPI: function(name) {
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
	},

	/*
	* Selection du genre d'impot
	*/
	selectGenreImpot: function(name, callback) {
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
	},

	/*
	* Selection du rattachement
	*/
	selectRattachement: function(name) {
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
	},

	/*
	* Annuler un for
	*/
	annulerFor: function(idFor) {
		if(confirm('Voulez-vous vraiment annuler ce for fiscal ?')) {
			Form.doPostBack("theForm", "annulerFor", idFor);
		}
	},

	/*
	* Annuler un for
	*/
	reOuvrirFor: function(idFor) {
		if(confirm('Voulez-vous vraiment ré-ouvrir ce for fiscal ?')) {
			Form.doPostBack("theForm", "reOuvrirFor", idFor);
		}
	},

	/**
	 * Construit la représentation Html de la table qui contient le résultat de simulation d'une action sur un for fiscal.
	 */
	buildActionTableHtml: function(results) {

		var table = '<table class="sync_actions" border="0" cellspacing="0"><tbody>';

		// header
		if (results.exception) {
			table += '<tr class="header"><td colspan="2">'
			table += 'Les erreurs de validation suivantes seront levées si vous confirmez les changements';
			table += '</tr>'
			table += '<tr class="exception" colspan="2">';
			table += '<td>' + StringUtils.escapeHTML(results.exception) + '</td>';
			table += '</tr>'
		}
		else if (results.errors && results.errors.length > 0) {
			table += '<tr class="header"><td colspan="2">'
			table += 'Les erreurs de validation suivantes seront levées si vous confirmez les changements';
			table += '</tr>'
			for (var i = 0; i < results.errors.length; ++i) {
				table += '<tr class="action">';
				table += '<td class="rowheader">»</td>';
				table += '<td class="error">' + StringUtils.escapeHTML(results.errors[i]) + '</td>';
				table += '</tr>'
			}
		}
		else {
			table += '<tr class="header"><td colspan="2">'
			table += 'Les actions suivantes seront exécutées si vous confirmez les changements'
			table += '</tr>'
			for (var i = 0; i < results.actions.length; ++i) {
				table += '<tr class="action">';
				table += '<td class="rowheader">»</td>';
				table += '<td class="action">' + StringUtils.escapeHTML(results.actions[i]) + '</td>';
				table += '</tr>'
			}
		}

		table += '</tbody></table>';

		return table;
	}

}

var quickSearchTarget = "/tiers/visu.do?id=";

var Quicksearch = {

	/**
	 * Détecte la pression de la touche 'enter' et navigue vers la page d'affichage
	 * du contribuable dont le numéro a été saisi. Les caractères non-numériques
	 * sont ignorés.
	 */
	onKeyPress: function(input, e) {
		var characterCode;

		if (e && e.which) {
			e = e;
			characterCode = e.which;
		} else {
			e = event;
			characterCode = e.keyCode;
		}

		if (characterCode == 13) {
			this.showCtb(input);
			return false;
		} else {
			return true;
		}
	},

	/**
	 * Navigue vers la page d'affichage du contribuable dont le numéro a été spécifié
	 */
	showCtb: function(input) {
		var value = new String(input.value);
		value = value.replace(/[^0-9]*/g, ''); // remove non-numeric chars
		var id = parseInt(value, 10);
		if (!isNaN(id)) {
			document.location = getContextPath() + quickSearchTarget + id;
		}
	},

	onFocus: function(input, invite) {
		if (input.value == invite) {
			input.value = "";
		}
		input.style.color = "black";
	},

	onBlur: function(input, invite) {
		if (input.value == "") {
			input.value = invite;
			input.style.color = "gray";
		}
	}

}

var Rapport = {
	/**
	* Affiche le taux d'activité seulement si le type d'activité est PRINCIPAL
	*/
	selectTypeActivite: function(type)
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
	},

	/*
	* Annuler un rapport
	*/
	annulerRapport: function(idRapport) {
		if(confirm('Voulez-vous vraiment annuler ce rapport entre tiers ?')) {
			Form.doPostBack("theForm", "annulerRapport", idRapport);
		}
	}
}


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

var Histo = {

	/**
	 * Affiche ou cache les lignes qui possèdent une date de fin.
	 *
	 * @tableId   l'id de la table
	 * @elementId l'id du checkbox d'affichage de l'historique
	 * @numCol    le numéro de la colonne (0-based) qui contient les dates de fin
	 * @cond      condition optionnelle de visibilité, appelée sur chaque ligne (true=visible selon algo, false=toujours invisible)
	 */
	toggleRowsIsHisto: function(tableId, elementId, numCol, cond) {

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
					else if (StringUtils.trim(x[numCol].innerHTML) == '' && x[numCol].innerHTML.indexOf('strike')== -1 && !this.hasClassName(tbl.rows[i], 'strike')) {
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
	},


	/*
	* Affichage des trois premières lignes d'un tableau ou de toutes les lignes en fonction du choix de l'utilisateur
	*/
	toggleAffichageRows: function(tableId, isAll, numCol) {

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
	},

	/*
	* Toggle rows is actif
	*/
	toggleRowsIsHistoPeriodicite: function(tableId,elementId, numCol,numColActive){

		var tbl = $('#' + tableId).get(0);
		if (tbl != null) {
			var len = tbl.rows.length;
			var showHisto = $('#' + elementId).attr('checked');

			for (i=1 ; i< len; i++){
				if (!showHisto) {
					var x = tbl.rows[i].cells;
					if ((StringUtils.trim(x[numCol].innerHTML) == '') && (!this.hasClassName(tbl.rows[i], 'strike'))||(x[numColActive].innerHTML.match('Active'))){
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
	},

	/*
	* Toggle rows is actif
	*/
	toggleRowsIsActif: function(tableId, elementId, numCol){

		var tbl = $('#' + tableId).get(0);
		if (tbl != null) {
			var len = tbl.rows.length;
			var showHisto = $('#' + elementId).attr('checked');

			for (i=1 ; i< len; i++){
				if (!showHisto) {
					var x = tbl.rows[i].cells;
					if ((x[numCol].innerHTML.indexOf('strike')== -1) && (!this.hasClassName(tbl.rows[i], 'strike'))){
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
	},

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
	hasClassName: function(objElement, strClass) {

		// if there is a class
		if ( objElement.className ) {

			// the classes are just a space separated list, so first get the list
			var arrList = objElement.className.split(' ');

			// get uppercase class for comparison purposes
			var strClassUpper = strClass.toUpperCase();

			// find all instances and remove them
			for ( var i = 0; i < arrList.length; i++ ) {
				// if class found
				if ( arrList[i].toUpperCase() == strClassUpper ) {
					// we found it
					return true;
				}
			}

		}

		// if we got here then the class name is not there
		return false;
	},

	/**
	 * Affiche ou filtre les données historiques d'une table
	 */
	refreshHistoTable: function(showHisto, table, dateFinIndex) {
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
}

var StringUtils = {

	trim: function(string) {
		if (string.trim) {
			return string.trim();
		}
		else {
			return string.replace(/^\s+|\s+$/g, "");
		}
	},

	isBlank: function(s) {
		return trim(s).length == 0;
	},

	isNotBlank: function(s) {
		return !isBlank(s);
	},

	isEmptyString: function(str) {
		return (!str || 0 === str.length);
	},

	isBlankString: function(str) {
		return (!str || /^\s*$/.test(str));
	},

	escapeHTML: function(text) {
		return $('<div/>').text(text).html();
	}
}

var DateUtils = {

	/*
	* Converti une chaine (dd.MM.yyyy) en date
	*/
	getDate: function(strDate, format){
		if (format == 'dd.MM.yyyy') {
			day = parseInt(strDate.substring(0,2));
			month = parseInt(strDate.substring(3,5));
			year = parseInt(strDate.substring(6,10));
		}
		else if (format == 'yyyy.MM.dd') {
			year = parseInt(strDate.substring(0,4));
			month = parseInt(strDate.substring(5,7));
			day = parseInt(strDate.substring(8,10));
		}
		else {
			alert("Type de format inconnu !");
		}
		d = new Date();
		d.setDate(day); // 1..31
		d.setMonth(month - 1); // 0..11
		d.setFullYear(year); // 4 digits
		return d;
	},

	/*
	* Ajoute nombreAnnees années à la date
	*/
	addYear: function(strDate, nombreAnnees, format){
		if (format == 'dd.MM.yyyy') {
			day = strDate.substring(0,2);
			month = strDate.substring(3,5);
			year = parseInt(strDate.substring(6,10)) + nombreAnnees;
		}
		if (format == 'yyyy.MM.dd') {
			year = parseInt(strDate.substring(0,4)) + nombreAnnees;
			month = strDate.substring(5,7);
			day = strDate.substring(8,10);
		}
		d = new Date();
		d.setDate(day);
		d.setMonth(month);
		d.setFullYear(year);
		return d;
	},

	/*
	* Retourne:
	*   0 si date_1=date_2
	  *   1 si date_1>date_2
	*  -1 si date_1<date_2
	*/
	compare: function(date_1, date_2){
	  diff = date_1.getTime()-date_2.getTime();
	  return (diff==0?diff:diff/Math.abs(diff));
	}

}

var App = {

	/**
	 * @return <b>true</b> si l'application courante est déployée en développement
	 */
	is_dev_env: function() {
		var url = window.location.toString();
		// toutes urls sur les ports 7001 (weblogic) ou 8080 (tomcat) sont considérées comme "développement"
		return url.match(/http:\/\/[.\w]+:7001\//) || url.match(/http:\/\/[.\w]+:8080\//);
	},

	/**
	 * (from http://jquery-howto.blogspot.com/2009/09/get-url-parameters-values-with-jquery.html)
	 *
	 * @return la map nom -> valeur des paramètres passés sur l'url.
	 */
	get_url_params: function()
	{
		var vars = [], hash;
		var hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');
		for(var i = 0; i < hashes.length; i++)
		{
			hash = hashes[i].split('=');
			vars.push(hash[0]);
			vars[hash[0]] = hash[1];
		}
		return vars;
	},

	/**
	 * Demande la confirmation à l'utilisateur avant de détruire les données de la
	 * base.
	 *
	 * @return <b>true</b> si l'utilisateur veut continuer, <b>false</b> autrement.
	 */
	confirm_trash_db: function() {
		if (this.is_dev_env()) {
			// A priori, un développeur sait ce qu'il fait...
			return true;
		}
		return confirm('Attention ! Cette opération va détruire les données existantes de la base.\n\nVoulez-vous vraiment continuer ?');
	},

	/**
	 * Execute l'action spécifiée sous forme d'url.
	 * <p>
	 * Les formats d'URL suivants sont supportés:
	 *   - "goto:/some/url/" : navige à l'url spécifiée
	 *   - "post:/some/other/url?param=machin" : exécute une requête HTML de type POST
	 */
	executeAction: function(url) {
		if (/^---/.test(url)) { // pattern de non-action -> rien à faire
			return false;
		}
		if (/^post:/.test(url)) { // requête de type POST
			var u = url.replace(/^post:/, '');
			var form = $('<form method="POST" action="' + getContextPath() + u + '"/>');
			form.appendTo('body');
			form.submit();
		}
		else if (/^goto:/.test(url)) { // requête de type GOTO
			var u = url.replace(/^goto:/, '');
			window.location.href = getContextPath() + u;
		}
	},

    /**
     * Redirige la page courante vers l'application (TAO, SIPF, CAT, ...) désirée
     */
    gotoExternalApp: function(select) {
    	var value = select.options[select.selectedIndex].value;
    	if ( value && value !== '') {
    		window.location.href = value;
    	}
    }

}

/**
 * Ces trois méthodes sont utilisées pour soumettre une forme avec un nom d'action (eventTarget) et
 * un argument (eventArgument). La forme doit posséder deux champs cachés nommés __TARGET__ et
 * __EVENT_ARGUMENT__. Il s'agit donc d'un mécanisme bizarre qui ne devrait plus être utilisé dans
 * les nouveaux écrans.
 */
var Form = {
	doPostBack : function(formName, eventTarget, eventArgument) {
		var theForm = $("form[name='" + formName + "']");
		if (theForm.length > 0) {
			$('input[name="__TARGET__"]', theForm).val(eventTarget);
			$('input[name="__EVENT_ARGUMENT__"]', theForm).val(eventArgument);
	        theForm.submit();
	    }
	},
	doAjaxActionPostBack : function(formName, event, eventTarget, eventArgument) {
		eventTarget = $(eventTarget);
		var eventId = eventTarget.attr('name');
		if (!eventId) {
			eventId = eventTarget.attr('id');
		}
		eventArgument = (!eventArgument ? {}: eventArgument);
		XT.doAjaxAction(eventId + event, eventTarget, eventArgument, {
			clearQueryString: true,
			formName: formName
		});
	},
	doAjaxSubmitPostBack : function(formName, event, eventTarget, eventArgument) {
		eventTarget = $(eventTarget);
		var eventId = eventTarget.attr('name');
		if (!eventId) {
			eventId = eventTarget.attr('id');
		}
		eventArgument = (!eventArgument ? {}: eventArgument);
		XT.doAjaxSubmit(eventId + event, eventTarget, eventArgument, {
			clearQueryString: true,
			formName:formName
		});
	}
};

var Tooltips = {

	/**
	 * Active les tooltips ajax sur tous les objets de la page ayant la classe 'jTip'.
	 *
	 * Exemple de tooltip ajax :
	 *
	 *     <span class="jTip" title="<c:url value="/htm/forPrincipalActif.htm?width=375"/>">?</span>
	 */
	activate_ajax_tooltips: function() {
		$(".jTip").tooltip({
			items: "[title]",
			content: function(response) {
				var url = $(this).attr("title");
				$.get(url, response);
				return "Chargement...";
			}
		});
	},

	/**
	  * Active les tooltips statiques sur tous les liens de la page ayant la classe 'staticTip'.
	 *
	 * Exemple de tooltip statique :
	 *
	 *     <a href="#tooltip" class="staticTip" id="link123">some link</a>
	 *     <div id="link123-tooltip" style="display:none;">
	 *         tooltip content goes here
	 *     </div>
	 */
	activate_static_tooltips: function() {
		$(".staticTip").tooltip({
			items: "[id]",
			content: function(response) {
				// on détermine l'id de la div qui contient le tooltip à afficher
				var id = $(this).attr("id") + "-tooltip";
				id = id.replace(/\./g, '\\.'); // on escape les points

				// on récupère la div et on affiche son contenu
				var div = $("#" + id);
				return div.attr("innerHTML");
			}
		});
	}
}

var Modifier = {

	formName : null,
	isModifiedSending : false,
	isModified : false,
	submitSaveName : "save",
	messageSaveSubmitConfirmation : "Voulez-vous vraiment sauver ?",
	messageResetSubmitConfirmation : "Voulez-vous vraiment annuler vos modification ?",
	messageOverConfirmation : "Voulez-vous vraiment quitter cette page sans sauver ?",
	inputTarget : null,
	isElementOver : function(element) {
		if ( element && element.tagName !== "A")
			return false;
		var link = element;
	    var href = link.href;
		return ( href != null && href !== "" && href.indexOf("#") <0  &&
	    		(link.target =='' || link.target =="_self") && link.onclick == null)
	},

	setIsModified : function( modified) {
		this.isModified = modified;
		this.onChange();
	},


	onChange : function() {
		this.inputTarget.value = this.isModified;
		if( this.isModified) {
			var form = document.forms[this.formName];
			var saveSubmit = form.elements[this.submitSaveName];
			if ( saveSubmit) saveSubmit.disabled = false;
		}
	},

	attachObserver : function( theForm, modified) {
		if (typeof theForm == 'string') {
			theForm = document.forms[theForm];
		}
	 	this.isModifiedSending = modified;
	 	this.formName = theForm.name;
	 	var count = theForm.elements.length;
	    var element;
	    var self = this;

	    $(theForm).submit(function(ev) {
			ev = ev || window.event;
			if (!self.submitSaveConfirmation(this)) {
				return Event.stop(ev);
			}
	    });

	    for (var i = 0; i < count; i++) {
	    	var element =  theForm.elements[i];
	        var tagName = element.tagName.toLowerCase();
	        if (tagName == "input") {
	            var type = element.type;
	            if ((type == "text" || type == "hidden" || type == "password")) {
	            	$(element).change(function() {
	                	self.setIsModified(true);
	                });
	                if (type == "text") {
						$(element).keyup(function(event) {
							event = event || window.event;
							return self.onkeyup( event);
						});
	                }
	            }
	            else if(type == "checkbox" || type == "radio"){
					$(element).click(function() {
						self.setIsModified(true);
					});
	            }
	            else if (type == "submit") {
	            	if (element.name === this.submitSaveName) {
	            		element.disabled = true;
	                }
	            }
	            else if (type == "reset") {
					$(element).click(function(ev) {
						ev = ev || window.event
						if (!self.submitResetConfirmation(this)) {
							return Event.stop(ev);
						}
					});
	            }
	        }
	        else if (tagName == "select") {
				$(element).change(function(ev) {
					self.setIsModified( true);
				});
	        }
	        else if (tagName == "textarea") {
	            $(element).change(function(ev) {
	             	self.setIsModified( true);
				});
	            $(element).keyup(function(event) {
					event = event || window.event;
					return self.onkeyup( event);
				});
	        }
	    }

	    var links = document.getElementsByTagName("A");
	    var count = links.length;
	    for (var i = 0; i < count; i++) {
	    	var link = links[i];
	    	 var href = link.href;
	    	if ( Modifier.isElementOver(link)) {
	    		var func =link.onclick;
	    		link.onclick =  function(ev) {
	             		ev = ev || window.event;
		            	if (!self.overConfirmation(this))
		            		return Event.stop(ev);
		            	if ( func) func();
	                }
	    	}
	    }

	    this.inputTarget = document.createElement("INPUT");
	    this.inputTarget.type = "hidden";
	    this.inputTarget.name = "__MODIFIER__";
	    theForm.appendChild( this.inputTarget);
	    this.setIsModified( this.isModifiedSending);
	},


	submitSaveConfirmation : function(submit) {
	  	if (!this.isModified || confirm(this.messageSaveSubmitConfirmation)) {
			var form = document.forms[this.formName];
			var saveSubmit = form.elements[this.submitSaveName];
			if (saveSubmit) {
				saveSubmit.disabled = true;
			}

			// d'où le "__confirmed_save" que l'on voit ensuite dans les contrôleurs...
			var elementName = "__confirmed_" + this.submitSaveName;
			var confirmedSave = form.elements[elementName];
			if (confirmedSave == null) {
				confirmedSave = $("<input name='" + elementName + "' type='hidden' value='yes'/>");
				$(confirmedSave).appendTo(form);
			}
			else {
				confirmedSave.value = "yes";
			}
			return true;
	  	}
	  	else {
	  		return false;
	  	}
	},

	overConfirmation : function(link) {
	  	if ( this.isModified)
	  		return confirm(this.messageOverConfirmation);
	  	return true;
	},

	submitResetConfirmation : function(reset) {
	  	if ( this.isModified) {
	  		if ( confirm(this.messageResetSubmitConfirmation)) {
	  			this.setIsModified( this.isModifiedSending);
	  			return true;
	  		}
	  		return false;
	  	}
	  	return true;
	},

	onkeyup : function(event) {
			var key = event.keyCode;
			//window.status = "keyCode: " + key;
			if ( key == Event.KEY_UP
					|| key == Event.KEY_LEFT
					|| key == Event.KEY_RIGHT
					|| key == Event.KEY_DOWN
					|| key == Event.KEY_RETURN
					|| key == Event.KEY_ESC) {
				// noop
			} else if ( key == Event.KEY_DELETE || key == Event.KEY_BACKSPACE || key > 31) {
				this.setIsModified( true);
			}
			return true
	  }
};

var Postit = {
	refresh : function() {
		$.get(getContextPath() + '/postit/todo.do?' + new Date().getTime() + '"/>', function(todo) {
			if (todo.taches > 0 || todo.dossiers > 0) {
				var text = 'Bonjour !<br>Il y a ';
				if (todo.taches > 0) {
					text += '<a href="../tache/list.do">' + todo.taches + ' tâche(s)</a>';
				}
				if (todo.taches > 0 && todo.dossiers > 0) {
					text += ' et ';
				}
				if (todo.dossiers > 0) {
					text += '<a href="../tache/list-nouveau-dossier.do">' + todo.dossiers + ' dossier(s)</a>';
				}
				text += ' en instance.';
				$('#postitText').html(text);
				$('#postit').show();
			}
			else {
				$('#postit').hide();
			}
		});
	}
}