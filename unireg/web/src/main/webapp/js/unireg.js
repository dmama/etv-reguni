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

//===================================================

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

						var buttonId = $(button).attr('id');

						var queryString = '/search/quick.do?query=' + encodeURIComponent(current);
						if (filter_bean) {
							queryString += '&filterBean=' + encodeURIComponent(filter_bean);
							queryString += '&filterParams=' + encodeURIComponent(filter_params);
						}
						queryString += '&' + new Date().getTime();

						// on effectue la recherche par ajax
						$.get(getContextPath() + queryString, function(results) {
							$('#tiers-picker-filter-description').text(results.filterDescription);
							$('#tiers-picker-results').html(Dialog.build_html_tiers_picker_results(results, buttonId));
						}, 'json')
						.error(Ajax.popupErrorHandler);

					}, 200); // 200 ms
					$.data(this, "tiers-picker-timer", timer);
				}
			});

			// on installe la fonction qui sera appelée lors de la demande de recherche avancée
			var fullSearch = $('#fullSearch').button();
			fullSearch.click(function() {

				var buttonId = $(button).attr('id');

				var queryString = '/search/full.do?';
				queryString += 'id=' + encodeURIComponent($('#tiers-picker-id').val());
				queryString += '&nomRaison=' + encodeURIComponent($('#tiers-picker-nomraison').val());
				queryString += '&localite=' + encodeURIComponent($('#tiers-picker-localite').val());
				queryString += '&dateNaissance=' + encodeURIComponent($('#tiers-picker-datenaissance').val());
				queryString += '&noAvs=' + encodeURIComponent($('#tiers-picker-noavs').val());
				if (filter_bean) {
					queryString += '&filterBean=' + encodeURIComponent(filter_bean);
					queryString += '&filterParams=' + encodeURIComponent(filter_params);
				}
 				queryString += '&' + new Date().getTime()

				// on effectue la recherche par ajax
				$.get(getContextPath() + queryString, function(results) {
					$('#tiers-picker-filter-description').text(results.filterDescription);
					$('#tiers-picker-results').html(Dialog.build_html_tiers_picker_results(results, buttonId));
				}, 'json')
				.error(Ajax.popupErrorHandler);
			});

			// la fonction pour tout effacer, y compris les résultat de la recherche
			var fullSearch = $('#clearAll').button();
			fullSearch.click(function() {
				$('#tiers-picker-id').val(null);
				$('#tiers-picker-nomraison').val(null);
				$('#tiers-picker-localite').val(null);
				$('#tiers-picker-datenaissance').val(null);
				$('#tiers-picker-noavs').val(null);
				$('#tiers-picker-results').html('');
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

	build_html_tiers_picker_results: function(results, buttonId) {
		var table = results.summary;

		if (results.entries.length > 0) {
			table += '<table border="0" cellspacing="0">';
			table += '<thead><tr class="header">';
			table += '<th>Numéro</th><th>Nom / Raison sociale</th><th>Date de naissance</th><th>Domicile</th><th>For principal</th>';
			table += '</tr></thead>';
			table += '<tbody>';
			for(var i = 0; i < results.entries.length; ++i) {
				var e = results.entries[i];
				table += '<tr class="' + (i % 2 == 0 ? 'even' : 'odd')  + '">';
				table += '<td><a onclick="document.getElementById(\'' + buttonId + '\').select_tiers_id(this); return false;" href="#">' + StringUtils.escapeHTML(e.numero) + '</a></td>';
				table += '<td>' + StringUtils.escapeHTML(e.nom1) + (e.nom2 ? ' ' + StringUtils.escapeHTML(e.nom2) : '' ) + '</td>';
				table += '<td>' + StringUtils.escapeHTML(e.dateNaissance) + '</td>';
				table += '<td>' + StringUtils.escapeHTML(e.domicile) + '</td>';
				table += '<td>' + StringUtils.escapeHTML(e.forPrincipal) + '</td>';
			}
			table += '</tbody></table>';
		}

		return table;
	},

	/**
	 * Ouvre une boîte de dialog modale qui affiches les dates de création/modification de l'élément spécifié.
	 *
	 * @param nature le type d'élément
	 * @param id     l'id de l'élément
	 */
	open_consulter_log: function(nature, id) {

		// charge le contenu de la boîte de dialogue
		$.getJSON(getContextPath() + '/common/consult-log.do?nature=' + nature + '&id=' + id + '&' + new Date().getTime(), function(view) {

			var dialog = Dialog.create_dialog_div('consulter-log-dialog');

			var utilisateurCreation = view.utilisateurCreation ? StringUtils.escapeHTML(view.utilisateurCreation) : '';
			var utilisateurDerniereModif = view.utilisateurDerniereModif ? StringUtils.escapeHTML(view.utilisateurDerniereModif) : '';
			var utilisateurAnnulation = view.utilisateurAnnulation ? StringUtils.escapeHTML(view.utilisateurAnnulation) : '';

			var dateHeureCreation = view.dateHeureCreation ? new Date(view.dateHeureCreation) : null;
			var dateHeureDerniereModif = view.dateHeureDerniereModif ? new Date(view.dateHeureDerniereModif) : null;
			var dateHeureAnnulation = view.dateHeureAnnulation ? new Date(view.dateHeureAnnulation) : null;

			dialog.html('<table>' +
				'<tr class="odd"><td width="25%">Utilisateur création&nbsp;:</td><td width="25%">' + utilisateurCreation + '</td>' +
				'<td width="25%">Date et heure création&nbsp;:</td><td width="25%">' +  DateUtils.toNormalString(dateHeureCreation) + '</td></tr>' +
				'<tr class="even"><td width="25%">Utilisateur dernière modification&nbsp;:</td><td width="25%">' + utilisateurDerniereModif + '</td>' +
				'<td width="25%">Date et heure dernière modification&nbsp;:</td><td width="25%">' + DateUtils.toNormalString(dateHeureDerniereModif) + '</td></tr>' +
				'<tr class="odd"><td width="25%">Utilisateur annulation&nbsp;:</td><td width="25%">' + utilisateurAnnulation + '</td>' +
				'<td width="25%">Date et heure annulation&nbsp;:</td><td width="25%">' + DateUtils.toNormalString(dateHeureAnnulation) + '</td></tr>' +
				'</table>');

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
			dialog.html(content); // on remplace le contenu de la boîte de dialogue
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
		dialog.load(getContextPath() + '/tiers/mouvement.do?idMvt=' + id + '&' + new Date().getTime());

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
			dialog.html('<img src="'+ getContextPath() + '/images/loading.gif"/>'); // on vide la boîte de dialogue de son contenu précédant
		}
		return dialog;
	}

}

//===================================================

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
		$.get(getContextPath() + '/fors/motifsOuverture.do?tiersId=' + numeroCtb + '&genreImpot=' + genreImpot + '&rattachement=' + rattachement + '&' + new Date().getTime(), function(motifs) {
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
		}, 'json')
		.error(Ajax.popupErrorHandler);
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
		$.get(getContextPath() + '/fors/motifsFermeture.do?tiersId=' + numeroCtb + '&genreImpot=' + genreImpot + '&rattachement=' + rattachement + '&' + new Date().getTime(), function(motifs) {
			var list = '<option></option>'; // dans le cas du motif de fermeture, on ajoute toujours une option vide
			for(var i = 0; i < motifs.length; ++i) {
				var motif = motifs[i];
				list += '<option value="' + motif.type + '"' + (motif.type == motifFermeture ? ' selected=true' : '') + '>' + StringUtils.escapeHTML(motif.label) + '</option>';
			}
			motifsFermetureSelect.html(list);
		}, 'json')
		.error(Ajax.popupErrorHandler);
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

//===================================================

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

//===================================================

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

//===================================================

/**
 * Classe utilitaire pour la manipulation de données en relation avec les tiers
 */
var Tiers = {
	/**
	 * Récupère des informations générales sur un tiers (voir la classe java TiersInfoController pour le détails des données retournées)
	 */
	queryInfo : function(numero, callback) {
		$.getJSON(getContextPath() + 'tiers/info.do?numero=' + numero + '&' + new Date().getTime(), callback, 'json').error(Ajax.popupErrorHandler);
	},

	/**
	 * Formatte un numéro de tiers pour l'affichage.
	 *
	 * Exemple : 54, 1.05, 123.34, 21.764.00, 120.223.344.
	 */
	formatNumero: function(numero) {
		var s = '';
		if (numero) {
			numero = '' + numero;
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
	},

	/**
	 * Effectue la validation (appel asynchrone) sur le tiers dont le numéro est spécifié, et retourne la liste des erreurs et des warnings à travers le callback spécifié.
	 */
	validate: function(numero, callback) {
		$.getJSON(getContextPath() + 'validation/tiers.do?id=' + numero + '&' + new Date().getTime(), callback, 'json').error(Ajax.notifyErrorHandler("validation du tiers"));
	},

	/**
	 * Effectue la validation (appel asynchrone) sur le tiers spécifié et affiche les éventuelles erreurs/warnings dans le div spécifié.
	 */
	loadValidationMessages: function(numero, div) {
		this.validate(numero, function(results) {
			var html = '';
			if (results.errors || results.warnings) {
				html += '<table class="validation_error" cellspacing="0" cellpadding="0" border="0">';
				html += '<tr><td class="heading">Un ou plusieurs problèmes ont été détectés sur ce contribuable ';
				html += '<span id="val_script">(<a href="#" onclick="$(\'#val_errors\').show(); $(\'#val_script\').hide(); return false;">voir le détail</a>)</span></td></tr>';
				html += '<tr id="val_errors" style="display:none"><td class="details"><ul>';

				if (results.errors) {
					for (var i in results.errors) {
						html += '<li class="err">Erreur: ' + StringUtils.escapeHTML(results.errors[i]) +'</li>'
					}
				}
				if (results.warnings) {
					for (var i in results.warnings) {
						html += '<li class="warn">Warning: ' + StringUtils.escapeHTML(results.warnings[i]) +'</li>'
					}
				}
				html += '</ul></td></tr></table>';
			}
			$(div).html(html);
		});
	}
}

//===================================================

var Link = {

	/**
	 * Génère le code html qui va bien pour afficher une icône de visualisation des logs d'un élément.
	 * @param nature la nature de l'entité
	 * @param id l'id de l'entité
	 * @return {String} le code html qui va bien.
	 */
	consulterLog:function (nature, id) {
		return '<a href="#" class="consult" title="Consultation des logs" onclick="return Dialog.open_consulter_log(\'' + nature + '\', ' + id + ');">&nbsp;</a>';
	},

	/**
	 * Remplace de manière temporaire un lien par un autre élément (par exemple, le même lien, mais désactivé).
	 * @param link un élément jquery
	 * @param replacement un élément de remplacement temporaire
	 */
	tempSwap:function (link, replacement) {
		$(link).hide();
		$(replacement).show();
		setTimeout(function () {
			$(link).show();
			$(replacement).hide();
		}, 2000);
	}
};

//===================================================

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

//===================================================

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
		return text ? $('<div/>').text(text).html() : '';
	},

	leftpad: function (val, len, car) {
		val = String(val);
		len = len || 2;
		car = car || ' ';
		while (val.length < len) val = car + val;
		return val;
	}
}

//===================================================

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
	},

	toCompactString: function(date) {
		if (!date) {
			return '';
		}

		if (this.isToday(date)) {
			return date.format('HH:MM:ss');
		}
		else {
			return date.format('dd.mm.yyyy HH:MM:ss');
		}
	},

	toNormalString: function(date) {
		if (!date) {
			return '';
		}

		return date.format('dd.mm.yyyy HH:MM:ss');
	},

	isToday: function(date) {
		return this.isSameDay(date, new Date());
	},

	isSameDay: function(left, right) {
		return left.getFullYear() == right.getFullYear() && left.getMonth() == right.getMonth() && left.getDate() && right.getDate();
	},

	durationToString: function(start, end) {
		end = end || new Date();
		var milliseconds = end.getTime() - start.getTime();

		var seconds = Math.floor((milliseconds / 1000) % 60);
		var minutes = Math.floor(((milliseconds / 1000) / 60) % 60);
		var hours = Math.floor(((milliseconds / 1000) / 3600) % 24);
		var days = Math.floor((milliseconds / 1000) / (3600 * 24));

		var duration;
		if (days == 0) {
			if (hours == 0) {
				if (minutes == 0) {
					duration = seconds + 's';
				}
				else {
					duration = minutes + 'm ' + StringUtils.leftpad(seconds, 2, '0') + 's';
				}
			}
			else {
				duration = hours + 'h ' + StringUtils.leftpad(minutes, 2, '0') + 'm ' + StringUtils.leftpad(seconds, 2, '0') + 's';
			}
		}
		else {
			duration = days + 'd ' + hours + 'h ' + StringUtils.leftpad(minutes, 2, '0') + 'm ' + StringUtils.leftpad(seconds, 2, '0') + 's';
		}

		return duration;
	}
}

var RegDate = {

	/**
	 * Converti une regdate (structure year/month/day) en date
	 * @param regdate
	 */
	toDate: function(regdate) {
		if (!regdate) {
			return null;
		}
		d = new Date();
		d.setDate(regdate.day); // 1..31
		d.setMonth(regdate.month - 1); // 0..11
		d.setFullYear(regdate.year); // 4 digits
		return d;
	},

	format: function(regdate, format) {
		var date = this.toDate(regdate);
		if (!date) {
			return '';
		}
		format = format || 'shortDate';
		return date.format(format);
	}
}
//===================================================

/*
 * Date Format 1.2.3
 * (c) 2007-2009 Steven Levithan <stevenlevithan.com>
 * MIT license
 *
 * Includes enhancements by Scott Trenda <scott.trenda.net>
 * and Kris Kowal <cixar.com/~kris.kowal/>
 *
 * Accepts a date, a mask, or a date and a mask.
 * Returns a formatted version of the given date.
 * The date defaults to the current date/time.
 * The mask defaults to dateFormat.masks.default.
 */

var dateFormat = function () {
	var	token = /d{1,4}|m{1,4}|yy(?:yy)?|([HhMsTt])\1?|[LloSZ]|"[^"]*"|'[^']*'/g,
		timezone = /\b(?:[PMCEA][SDP]T|(?:Pacific|Mountain|Central|Eastern|Atlantic) (?:Standard|Daylight|Prevailing) Time|(?:GMT|UTC)(?:[-+]\d{4})?)\b/g,
		timezoneClip = /[^-+\dA-Z]/g,
		pad = function (val, len) {
			val = String(val);
			len = len || 2;
			while (val.length < len) val = "0" + val;
			return val;
		};

	// Regexes and supporting functions are cached through closure
	return function (date, mask, utc) {
		var dF = dateFormat;

		// You can't provide utc if you skip other args (use the "UTC:" mask prefix)
		if (arguments.length == 1 && Object.prototype.toString.call(date) == "[object String]" && !/\d/.test(date)) {
			mask = date;
			date = undefined;
		}

		// Passing date through Date applies Date.parse, if necessary
		date = date ? new Date(date) : new Date;
		if (isNaN(date)) throw SyntaxError("invalid date");

		mask = String(dF.masks[mask] || mask || dF.masks["default"]);

		// Allow setting the utc argument via the mask
		if (mask.slice(0, 4) == "UTC:") {
			mask = mask.slice(4);
			utc = true;
		}

		var	_ = utc ? "getUTC" : "get",
			d = date[_ + "Date"](),
			D = date[_ + "Day"](),
			m = date[_ + "Month"](),
			y = date[_ + "FullYear"](),
			H = date[_ + "Hours"](),
			M = date[_ + "Minutes"](),
			s = date[_ + "Seconds"](),
			L = date[_ + "Milliseconds"](),
			o = utc ? 0 : date.getTimezoneOffset(),
			flags = {
				d:    d,
				dd:   pad(d),
				ddd:  dF.i18n.dayNames[D],
				dddd: dF.i18n.dayNames[D + 7],
				m:    m + 1,
				mm:   pad(m + 1),
				mmm:  dF.i18n.monthNames[m],
				mmmm: dF.i18n.monthNames[m + 12],
				yy:   String(y).slice(2),
				yyyy: y,
				h:    H % 12 || 12,
				hh:   pad(H % 12 || 12),
				H:    H,
				HH:   pad(H),
				M:    M,
				MM:   pad(M),
				s:    s,
				ss:   pad(s),
				l:    pad(L, 3),
				L:    pad(L > 99 ? Math.round(L / 10) : L),
				t:    H < 12 ? "a"  : "p",
				tt:   H < 12 ? "am" : "pm",
				T:    H < 12 ? "A"  : "P",
				TT:   H < 12 ? "AM" : "PM",
				Z:    utc ? "UTC" : (String(date).match(timezone) || [""]).pop().replace(timezoneClip, ""),
				o:    (o > 0 ? "-" : "+") + pad(Math.floor(Math.abs(o) / 60) * 100 + Math.abs(o) % 60, 4),
				S:    ["th", "st", "nd", "rd"][d % 10 > 3 ? 0 : (d % 100 - d % 10 != 10) * d % 10]
			};

		return mask.replace(token, function ($0) {
			return $0 in flags ? flags[$0] : $0.slice(1, $0.length - 1);
		});
	};
}();

// Some common format strings
dateFormat.masks = {
	"default":      "dd.mm.yyyy HH:MM:ss",
	shortDate:      "dd.mm.yyyy",
	mediumDate:     "mmm d, yyyy",
	longDate:       "mmmm d, yyyy",
	fullDate:       "dddd, mmmm d, yyyy",
	shortTime:      "h:MM TT",
	mediumTime:     "h:MM:ss TT",
	longTime:       "h:MM:ss TT Z",
	isoDate:        "yyyy-mm-dd",
	isoTime:        "HH:MM:ss",
	isoDateTime:    "yyyy-mm-dd'T'HH:MM:ss",
	isoUtcDateTime: "UTC:yyyy-mm-dd'T'HH:MM:ss'Z'"
};

// Internationalization strings
dateFormat.i18n = {
	dayNames: [
		"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat",
		"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
	],
	monthNames: [
		"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
		"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"
	]
};

// For convenience...
Date.prototype.format = function (mask, utc) {
	return dateFormat(this, mask, utc);
};

//===================================================

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

//===================================================

var Ajax = {

	init: function() {
		// Ecoute les événements globaux ajax pour afficher et masquer l'image de chargement
		$("#loadingImage").bind("ajaxStart", function() {
			$(this).show();
		}).bind("ajaxStop", function(){
			$(this).hide();
		});
	},

    /**
     * Error handler qui affiche le message d'erreur ajax dans une boîte de dialogue modale.
     */
    popupErrorHandler: function(xhr, ajaxOptions, thrownError) {
		alert("Désolé ! Une erreur est survenue et l'action demandée n'a pas pu être effectuée.\n\n" +
			"Veuillez réessayer plus tard, s'il-vous-plaît.\n\n" +
			"Si le problème persiste, merci de communiquer à votre administrateur le message suivant :\n\n" +
			"\t" + thrownError + ' (' +  xhr.status +') : '+ xhr.responseText);
    },

    /**
     * Error handler qui affiche le message d'erreur ajax dans une notification non-modale.
     */
    notifyErrorHandler: function(action) {
    	return function(xhr, ajaxOptions, thrownError) {
			$.jGrowl("Désolé ! Une erreur est survenue et l'action <i>" + action + "</i> n'a pas pu être effectuée.<br/><br/>" +
				"Veuillez réessayer plus tard, s'il-vous-plaît.<br/><br/>" +
				"Si le problème persiste, merci de communiquer à votre administrateur le message suivant :<br/><br/>" +
				"&nbsp;&nbsp;&nbsp;&nbsp;<i>" + StringUtils.escapeHTML(thrownError) + ' (' +  StringUtils.escapeHTML(xhr.status) +') : '+
				StringUtils.escapeHTML(xhr.responseText) + '</i>',
				{life:10000});
		}
    }
}


//===================================================

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
	}
};

//===================================================

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
	 * @param obj l'élément parent à partir duquel les tooltips doivent être activés (optionnel).
	 *            Si par renseigné, applique les tooltips sur toute la page.
	 */
	activate_static_tooltips: function(obj) {
		$(".staticTip", obj).tooltip({
			items: "[id]",
			content: function(response) {
				// on détermine l'id de la div qui contient le tooltip à afficher
				var id = $(this).attr("id") + "-tooltip";
				id = id.replace(/\./g, '\\.'); // on escape les points

				// on récupère la div et on affiche son contenu
				var div = $("#" + id);
				return div.html();
			}
		});
	}
}

//===================================================

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

//===================================================

var Postit = {
	refresh : function() {
		$.get(getContextPath() + '/postit/todo.do?' + new Date().getTime(), function(todo) {
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
		}, 'json')
		.error(Ajax.notifyErrorHandler("affichage du post-it"));
	}
}

//===================================================

var Batch = {
	loadRunning: function(div, refreshInterval, readonly) {
		var requestDone = true;
		$(document).everyTime(refreshInterval, function() {
			if (!requestDone) {
				return;
			}
			requestDone = false;

			$.get(getContextPath() + '/admin/batch/running.do?' + new Date().getTime(), function(jobs) {
				var h = Batch.__buildHtmlTableRunningBatches(jobs, readonly);
				$("#jobsActif").html(h);
				requestDone = true;
			}, 'json')
			.error(function(xhr, ajaxOptions, thrownError) {
				var message = '<span class="error">Oups ! Le chargement de la liste des batches en cours a provoqué l\'erreur suivante :' +
					'&nbsp;<i>' + StringUtils.escapeHTML(thrownError) + ' (' +  StringUtils.escapeHTML(xhr.status) + ') : ' + StringUtils.escapeHTML(xhr.responseText) + '</i></span>';
				$("#jobsActif").html(message);
				requestDone = true;
			});
		});
	},

	start: function(name) {

		// On désactive temporairement le bouton de démarrage pour donner un feedback que le clic a bien été enregistré
		var startButton = $('#start' + name);
		startButton.attr('disabled', 'disabled');
		startButton.val('Démarrage...');
		setTimeout(function() {
			startButton.removeAttr('disabled');
			startButton.val('Démarrer le batch');
		}, 2500); // 2.5s, le temps que l'affichage des batches en cours s'actualise

		var form = $('#' + name);
		form.attr('action', getContextPath() + '/admin/batch/start.do?name=' + encodeURIComponent(name));
		// cet appel nécessite la plugin jquery.form.js pour gérer l'upload ajax de fichiers dans les formulaires (voir http://malsup.com/jquery/form/)
		form.ajaxSubmit({
			success: function(responseText, statusText) {
				if (responseText) {
					responseText = responseText.replace(/(<pre>|<\/pre>)/ig, ''); // enlève les éventuelles balises <pre> qui apparaissent des fois avec Firefox et IE
				}
				if (responseText) {
					alert(responseText);
				}
			},
			error: function(jqXHR, textStatus, errorThrown) {
				alert('Une erreur est survenue lors du démarrage du batch. Si l\'erreur se reproduit, merci de de transmettre le code d\'erreur ci-dessous à votre administrateur:\n\n' +
					textStatus + ': ' + errorThrown);
			}
		});
	},

	stop: function(name) {
		$.post(getContextPath() + '/admin/batch/stop.do?name=' + encodeURIComponent(name), function(returnCode) {
			if (returnCode) {
				alert(returnCode);
			}
		});
	},

	__buildHtmlTableRunningBatches: function(jobs, readonly) {
		var table = '<table>';
		table += '<thead><tr><th>Action</th><th>Nom</th><th>Progression</th><th>Statut</th><th>Début</th><th>Durée</th></tr></thead>';
		table += '<tbody>';

		for(var i = 0; i < jobs.length; ++i) {
			var job = jobs[i];
			var lastStart = job.lastStart ? new Date(job.lastStart) : null;
			var lastEnd = job.lastEnd ? new Date(job.lastEnd) : null;
			var isRunning = job.status == 'JOB_RUNNING';

			// general description + status
			table += '<tr class="' + (i % 2 == 0 ? 'even' : 'odd')  + '">';

			if (!isRunning || readonly) {
				table += '<td></td>';
			}
			else {
				table += '<td><a class="stop iepngfix" href="#" onclick="return Batch.stop(\'' + job.name + '\');"></a></td>';
			}

			table += '<td>' + StringUtils.escapeHTML(job.description) + '</td>';
			table += '<td>' + (job.runningMessage ? StringUtils.escapeHTML(job.runningMessage) : '') + '</td>';

			if (job.percentProgression) {
				table += '<td align="left">' + this.__renderPercent(100, job.percentProgression) + '</td>';
			}
			else {
				table += '<td align="left">' + StringUtils.escapeHTML(this.__statusDescription(job.status)) + '</td>';
			}

			table += '<td nowrap="nowrap">' + DateUtils.toCompactString(lastStart) + '</td>';

			if (lastStart) {
				table += '<td nowrap="nowrap">' + DateUtils.durationToString(lastStart, lastEnd) + '</td>';
			}
			else {
				table += '<td nowrap="nowrap"></td>';
			}
			table += '</tr>';

			// detailed parameters
			if (job.runningParams) {
				var params = '<tr class="' + (i % 2 == 0 ? 'even' : 'odd')  + '">';
				params += '<td>&nbsp;</td>';
				params += '<td colspan="5">';

				params += '<table class="jobparams"><tbody>';

				var hasParam = false;
				for(key in job.runningParams) {
					var value = job.runningParams[key];
					params += '<tr><td>' + StringUtils.escapeHTML(key) + '</td><td>➭ ' + StringUtils.escapeHTML(value) + '</td></tr>';
					hasParam = true;
				}

				params += '</tbody></table>';

				params += '</td>';
				params += '</tr>';

				if (hasParam) {
					table += params;
				}
			}
		}

		table += '</tbody></table>';
		return table;
	},

	__renderPercent: function(width, percent) {
		var pixels = Math.floor((width * percent) / 100);
		var html = '<div class="progress-bar" style="width:' + width + 'px">';
		html += '<div class="progress-bar-fill" style="width: ' + pixels + 'px"></div>';
		html += '<div class="progress-bar-text" style="width: ' + width + 'px">' + percent + '%</div></div>';
		return html;
	},

	__statusDescription: function(status) {
		if (status == 'JOB_OK') {
			return 'OK';
		}
		else if (status == 'JOB_READY') {
			return 'Prêt';
		}
		else if (status == 'JOB_RUNNING') {
			return 'En cours';
		}
		else if (status == 'JOB_EXCEPTION') {
			return 'Exception';
		}
		else if (status == 'JOB_INTERRUPTING') {
			return 'En cours d\'interruption';
		}
		else if (status == 'JOB_INTERRUPTED') {
			return 'Interrompu';
		}
		else {
			return '';
		}
	}
}

//===================================================

var DisplayTable = {

	buildPagination:function (page, pageSize, totalCount, buildGotoPageStatement) {

		var html = '';

		if (totalCount > pageSize) {
			html += '<table class="pageheader" style="margin-top: 0px;"><tr>\n';
			html += '<td class="pagebanner">Trouvé ' + totalCount + ' éléments. Affichage de ' + ((page - 1) * pageSize + 1) + ' à ' + (page * pageSize) + '.</td>';
			html += '<td class="pagelinks">&nbsp;\n';

			var pageCount = Math.ceil(totalCount / pageSize);
			var firstShownPage = Math.max(1, page - 5);
			var lastShownPage = Math.min(pageCount, page + 5);

			// previous link
			if (page > 1) {
				html += '<a href="#" onclick="' + buildGotoPageStatement(1) + '; return false;">«&nbsp;premier</a>\n';
				html += '<a href="#" onclick="' + buildGotoPageStatement(page - 1) + '; return false;">‹&nbsp;précédent</a>\n';
			}

			// direct page links
			for (var i = firstShownPage; i <= lastShownPage; ++i) {
				if (i == page) {
					html += '<font size="+1"><strong>' + i + '</strong></font>&nbsp;\n';
				}
				else {
					html += '<a href="#" onclick="' + buildGotoPageStatement(i) + '; return false;">' + i + '</a>&nbsp;\n';
				}
			}

			// next link
			if (page < pageCount) {
				html += '<a href="#" onclick="' + buildGotoPageStatement(page + 1) + '; return false;">suivant&nbsp;›</a>\n';
				html += '<a href="#" onclick="' + buildGotoPageStatement(pageCount) + '; return false;">dernier&nbsp;»</a>\n';
			}

			html += '</td></tr></table>';
		}
		else if (totalCount == 0) {
			html += '<table class="pageheader" style="margin-top: 0px;"><tr>\n';
			html += '<td class="pagebanner">Aucun élément trouvé.</td>';
			html += '</td></tr></table>';
		}

		return html;
	}
};

//===================================================

var Inbox = {

	requestInboxSizeDone: true,

	/**
	 * Cette méthode met-à-jour le nombre d'éléments non-lus de l'inbox de manière asynchrone (ajax).
	 *
	 * @param span le span qui contient le text à mettre-à-jour
	 * @param text le texte de base de l'inbox (p.a. "Boîte de réception") auquel sera ajouté le nombre d'éléments non-lus (pour devenir "Boîte de réception (2)", par exemple)
	 */
	refreshSize: function(span, text) {

		if (!this.requestInboxSizeDone) {
			return;
		}
		this.requestInboxSizeDone = false;

		$.get(getContextPath() + "/admin/inbox/unreadSize.do?" + new Date().getTime(), function(unreadSize) {
			if (unreadSize > 0) {
				$(span).text(text + ' (' + unreadSize + ')');
				$(span).attr('style', 'font-weight: bold');
			}
			else {
				$(span).text(text);
				$(span).attr('style', '');
			}
			this.requestInboxSizeDone = true;
		}, 'json')
		.error(Ajax.notifyErrorHandler("recherche du nombre d'éléments dans la boîte de réception"));
	}
};

//===================================================

var Decl = {
	/**
	 * Cette méthode ouvre un fenêtre popup avec les détails (read-only) de la déclaration d'impôt ordinaire (DI) dont l'id est passé en paramètre.
	 * @param diId l'id de la déclaration à afficher.
	 */
	open_details_di: function(diId) {

		$.getJSON(getContextPath() + "/decl/details.do?id=" + diId + "&" + new Date().getTime(), function(di) {
			if (di) {
				var info = '<fieldset class="information"><legend><span>Caractéristiques de la déclaration d\'impôt</span></legend>';
				info += '<table><tr class="odd"><td width="25%">Période fiscale&nbsp;:</td><td width="25%">' + di.periodeFiscale + '</td>';
				info += '<td width="25%">Code contrôle&nbsp;:</td><td width="25%">' + StringUtils.escapeHTML(di.codeControle) + '</td></tr>';
				info += '<tr class="even"><td width="25%">Début période imposition&nbsp;:</td><td width="25%">' + RegDate.format(di.dateDebut) + '</td>';
				info += '<td width="25%">Fin période imposition&nbsp;:</td><td width="25%">' + RegDate.format(di.dateFin) + '</td></tr>';
				info += '<tr class="odd"><td width="25%">Type déclaration&nbsp;:</td><td width="25%">' + StringUtils.escapeHTML(di.typeDocumentMessage) + '</td>';
				info += '<td width="25%">&nbsp;</td><td width="25%">&nbsp;</td></tr></table></fieldset>\n';

				var delais = Decl._buildDelaisTable(di.delais);
				var etats = Decl._buidlEtatsTable(di.etats);

				var dialog = Dialog.create_dialog_div('details-di-dialog');
				dialog.html(info + delais + etats);

				dialog.dialog({
					title: "Détails de la déclaration d'impôt",
					width: 650,
					modal: true,
					buttons: {
						Ok: function() {
							dialog.dialog("close");
						}
					}
				});
			}
			else {
				alert("La déclaration n'existe pas.");
			}
		})
		.error(Ajax.notifyErrorHandler("affichage des détails de la déclaration"));
	},

	/**
	 * Cette méthode ouvre un fenêtre popup avec les détails (read-only) de la déclaration d'impôt source (LR) dont l'id est passé en paramètre.
	 * @param lrId l'id de la déclaration à afficher.
	 */
	open_details_lr: function(lrId) {

		$.getJSON(getContextPath() + "/decl/details.do?id=" + lrId + "&" + new Date().getTime(), function(lr) {
			if (lr) {
				var info = '<fieldset class="information"><legend><span>Caractéristiques de la liste récapitulative</span></legend>';
				info += '<table><tr class="odd"><td width="50%">Date début période&nbsp;:</td><td width="50%">' + RegDate.format(lr.dateDebut) + '</td></tr>';
				info += '<tr class="even"><td width="50%">Date fin période&nbsp;:</td><td width="50%">' + RegDate.format(lr.dateFin) + '</td></tr></table></fieldset>\n';

				var delais = Decl._buildDelaisTable(lr.delais);
				var etats = Decl._buidlEtatsTable(lr.etats);

				var dialog = Dialog.create_dialog_div('details-di-dialog');
				dialog.html(info + delais + etats);

				dialog.dialog({
					title: "Détails de la déclaration d'impôt source",
					width: 650,
					modal: true,
					buttons: {
						Ok: function() {
							dialog.dialog("close");
						}
					}
				});
			}
			else {
				alert("La déclaration n'existe pas.");
			}
		})
		.error(Ajax.notifyErrorHandler("affichage des détails de la déclaration"));
	},

	_buildDelaisTable: function (delais) {
		var html = '';
		if (delais) {
			html = '<fieldset><legend><span>Délais</span></legend>';
			html +=
				'<table id="delai" class="display"><thead><tr><th>Date demande</th><th>Délai accordé</th><th>Confirmation éditée</th><th>Date traitement</th><th></th></tr></thead><tbody>';
			for (var i in delais) {
				var d = delais[i];
				html += '<tr class="' + (i % 2 == 0 ? 'even' : 'odd') + (d.annule ? ' strike' : '') + '">';
				html += '<td>' + RegDate.format(d.dateDemande) + '</td><td>' + RegDate.format(d.delaiAccordeAu) + '</td>';
				html += '<td>';
				if (d.confirmationEcrite) {
					html += '<input type="checkbox" checked="checked" disabled="disabled">';
					html += '<a href="' + getContextPath() + '/declaration/copie-conforme-delai.do?idDelai=' + d.id + '" class="pdf" id="print-delai-' + d.id +
						'" onclick="Link.tempSwap(this, \'#disabled-print-delai-' + d.id + '\');">&nbsp;</a>';
					html += '<span class="pdf-grayed" id="disabled-print-delai-' + d.id + '" style="display:none;">&nbsp;</span>';
				}
				else {
					html += '<input type="checkbox" disabled="disabled">';
				}
				html += '</td>';
				var logModifDate = d.logModifDate ? new Date(d.logModifDate) : null;
				html += '<td>' + RegDate.format(d.dateTraitement) + '</td><td style="action"><img src="../images/consult_off.gif" title="' + d.logModifUser + '-' +
					DateUtils.toNormalString(logModifDate) + '"></td></tr>';
			}
			html += '</tbody></table></fieldset>\n';
		}
		return html;
	},

	_buidlEtatsTable: function (etats) {
		var html = '';
		if (etats) {
			html = '<fieldset><legend><span>Etats</span></legend>';
			html += '<table id="etat" class="display"><thead><tr><th>Date</th><th>Etat</th><th>Source</th><th></th></tr></thead><tbody>';
			for (var i in etats) {
				var e = etats[i];
				html += '<tr class="' + (i % 2 == 0 ? 'even' : 'odd') + (e.annule ? ' strike' : '') + '">';
				html += '<td>' + RegDate.format(e.dateObtention);
				if (!e.annule && e.etat == 'SOMMEE') {
					html += '&nbsp;' + StringUtils.escapeHTML(e.dateEnvoiCourrierMessage);
				}
				html += '</td><td>' + StringUtils.escapeHTML(e.etatMessage);
				if (!e.annule && e.etat == 'SOMMEE') {
					html += '&nbsp;' + '<a href="' + getContextPath() + '/declaration/copie-conforme-sommation.do?idEtat=' + e.id + '" class="pdf" id="copie-sommation-' + e.id +
						'" onclick="Link.tempSwap(this, \'#disabled-copie-sommation-' + e.id + '\');">&nbsp;</a>';
					html += '<span class="pdf-grayed" id="disabled-copie-sommation-' + e.id + '" style="display:none;">&nbsp;</span>';
				}
				html += '</td><td>';
				if (e.etat == 'RETOURNEE') {
					html += StringUtils.escapeHTML(e.sourceMessage);
				}
				html += '</td><td>' + Link.consulterLog('EtatDeclaration', e.id) + '</td></tr>';
			}
		}
		return html;
	}

};