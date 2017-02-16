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

		input = $(input);

		// [SIFISC-832] on évite de passer par des variables locales parce qu'on cas de réinstanciation de l'autocompletion sur un champ,
		// cela crée plusieurs closures avec chacunes leurs variables locales. Et après, il y a plusieurs versions de l'event handler sur 'focusout'
		// et seulement une seule qui voit le bon item sélectionné.
		//var is_open = false;
		//var selected = null;

		input.autocomplete({
			source: App.curl(url),
			open: function() {
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
			close: function() {
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
			/** @namespace item.desc */
			return $( "<li></li>" )
				.data( "item.autocomplete", item )
				.append( "<a>" + item.desc + "</a>" )
				.appendTo( ul );
		};

		input.focusout(function() {
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
		input.keyup(function() {
			var current = input.val();
			if (current != previous) {
				previous = current;
				// on met-à-null la valeur chaque fois que le texte de saisie change
				input.data('selected', null);
			}
		});
	}
};

//===================================================

//noinspection JSUnusedGlobalSymbols
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
		var url = App.curl("/tiers/picker/tiers-picker.do");
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

						var queryString = '/search/quick.do?query=' + StringUtils.encodeURIComponent(current);
						if (filter_bean) {
							queryString += '&filterBean=' + StringUtils.encodeURIComponent(filter_bean);
							queryString += '&filterParams=' + StringUtils.encodeURIComponent(filter_params);
						}
						queryString += '&' + new Date().getTime();

						// on effectue la recherche par ajax
						$.get(App.curl(queryString), function(results) {
							/** @namespace results.filterDescription */
							$('#tiers-picker-filter-description').text(results.filterDescription);
							$('#tiers-picker-results').html(Dialog.__buildHtmlTiersPickerResults(results, buttonId));
						}, 'json')
						.error(Dialog.__tiersPickerErrorHandler);

					}, 200); // 200 ms
					$.data(this, "tiers-picker-timer", timer);
				}
			});

			// on installe la fonction qui sera appelée lors de la demande de recherche avancée
			var fullSearch = $('#fullSearch').button();
			fullSearch.click(function() {

				var buttonId = $(button).attr('id');

				var queryString = '/search/full.do?';
				queryString += 'id=' + StringUtils.encodeURIComponent($('#tiers-picker-id').val());
				queryString += '&nomRaison=' + StringUtils.encodeURIComponent($('#tiers-picker-nomraison').val());
				queryString += '&localite=' + StringUtils.encodeURIComponent($('#tiers-picker-localite').val());
				queryString += '&dateNaissance=' + StringUtils.encodeURIComponent($('#tiers-picker-datenaissance').val());
				queryString += '&noAvs=' + StringUtils.encodeURIComponent($('#tiers-picker-noavs').val());
				if (filter_bean) {
					queryString += '&filterBean=' + StringUtils.encodeURIComponent(filter_bean);
					queryString += '&filterParams=' + StringUtils.encodeURIComponent(filter_params);
				}
 				queryString += '&' + new Date().getTime();

				// on effectue la recherche par ajax
				$.get(App.curl(queryString), function(results) {
					$('#tiers-picker-filter-description').text(results.filterDescription);
					$('#tiers-picker-results').html(Dialog.__buildHtmlTiersPickerResults(results, buttonId));
				}, 'json')
				.error(Dialog.__tiersPickerErrorHandler);
			});

			// la fonction pour tout effacer, y compris les résultat de la recherche
			var clearAll = $('#clearAll').button();
			clearAll.click(function() {
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

	__tiersPickerErrorHandler: function(xhr, ajaxOptions, thrownError) {
		$('#tiers-picker-filter-description').text('');

		var html = "<span class=\"error\">Désolé ! Une erreur est survenue et la recherche demandée n'a pas pu être effectuée.\n\n" +
			"Veuillez réessayer plus tard, s'il-vous-plaît.<br><br>" +
			"Si le problème persiste, merci de communiquer à votre administrateur le message suivant :<br><br>" +
			"<ul style=\"padding-left:40px;\"><li>Date : " + new Date() + "</li>" +
			"<li>URL : " + StringUtils.escapeHTML(this.url) + "</li></ul><br>" +
			"" + StringUtils.escapeHTML(thrownError + ' (' +  xhr.status +') : '+ xhr.responseText) + "</span>";

		$('#tiers-picker-results').html(html);
	},

	__buildHtmlTiersPickerResults: function(results, buttonId) {
		var table = results.summary;

		/** @namespace results.entries */
		if (results.entries.length > 0) {
			table += '<table border="0" cellspacing="0">';
			table += '<thead><tr class="header">';
			table += '<th>Numéro</th><th>Nom / Raison sociale</th><th>Date de naissance</th><th>Domicile</th><th>For principal</th>';
			table += '</tr></thead>';
			table += '<tbody>';
			for(var i = 0; i < results.entries.length; ++i) {
				var e = results.entries[i];
				/** @namespace e.numero */
				/** @namespace e.nom1 */
				/** @namespace e.nom2 */
				/** @namespace e.dateNaissance */
				/** @namespace e.npa */
				/** @namespace e.localitePays */
				/** @namespace e.forPrincipal */
				table += '<tr class="' + (i % 2 == 0 ? 'even' : 'odd')  + '">';
				var onclick = "document.getElementById('" + buttonId + "').select_tiers_id(this); return false;";
				table += '<td><a onclick="' + onclick + '" href="#">' + Tiers.formatNumero(e.numero) + '</a></td>';
				table += '<td>' + StringUtils.escapeHTML(e.nom1) + (e.nom2 ? ' ' + StringUtils.escapeHTML(e.nom2) : '' ) + '</td>';
				table += '<td>' + StringUtils.escapeHTML(e.dateNaissance) + '</td>';
				table += '<td>' + (e.npa ? StringUtils.escapeHTML(e.npa) : '') + (e.localitePays ? ' ' + StringUtils.escapeHTML(e.localitePays) : '') + '</td>';
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
		$.getJSON(App.curl('/common/consult-log.do?nature=') + nature + '&id=' + id + '&' + new Date().getTime(), function(view) {

			/** @namespace view.utilisateurCreation */
			/** @namespace view.utilisateurDerniereModif */
			/** @namespace view.utilisateurAnnulation */
			/** @namespace view.dateHeureCreation */
			/** @namespace view.dateHeureDerniereModif */
			/** @namespace view.dateHeureAnnulation */

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

		$.getJSON(App.curl("/tiers/mouvement.do?idMvt=") + id + "&" + new Date().getTime(), function(mvt) {
			if (mvt) {
				/** @namespace mvt.dateExecution */
				/** @namespace mvt.executant */
				/** @namespace mvt.dateMouvement */
				/** @namespace mvt.collectiviteAdministrative */
				/** @namespace mvt.nomPrenomUtilisateur */
				/** @namespace mvt.typeMouvement */
				/** @namespace mvt.numeroTelephoneUtilisateur */

				var html =
				'<fieldset class="information">' +
					'<legend><span>Caractéristiques du mouvement du dossier</span></legend>' +
					'<table>' +
						'<tr class="odd" >'+
							'<td width="25%">Type de mouvement&nbsp;:</td>' +
							'<td width="25%">' + mvt.typeMouvement + '</td>' +
							'<td width="25%">Date du mouvement&nbsp;</td>' +
							'<td width="25%">' + RegDate.format(mvt.dateMouvement) + '</td>' +
						'</tr>' +
						'<tr class="even" >' +
							'<td width="25%">Exécutant&nbsp;:</td>' +
							'<td width="25%">' +mvt.executant + '</td>' +
							'<td width="25%">Date / Heure exécution&nbsp;:</td>' +
							'<td width="25%">' + mvt.dateExecution  + '</td>' +
						'</tr>' +
						'<tr class="odd" >' +
							'<td width="25%">Collectivité administrative&nbsp;:</td>' +
							'<td width="25%">' + mvt.collectiviteAdministrative + '</td>' +
							'<td width="25%">&nbsp;</td>' +
							'<td width="25%">&nbsp;</td>' +
						'</tr>' +
					'</table>' +
				'</fieldset>';

				//Utilisateur
				html +=
				'<fieldset>' +
					'<legend><span>Coordonnées de l\'utilisateur</span></legend>' +
					'<table>' +
						'<tr class="even" >' +
							'<td width="25%">Prénom / Nom&nbsp;:</td>' +
							'<td width="25%">' + mvt.nomPrenomUtilisateur +'</td>' +
							'<td width="25%">N° de téléphone fixe&nbsp;:</td>' +
							'<td width="25%">' + mvt.numeroTelephoneUtilisateur + '</td>' +
						'</tr>' +
					'</table>' +
				'</fieldset>';

				var dialog = Dialog.create_dialog_div('details-mouvement-dialog');
				dialog.html(html);
				dialog.dialog({
					title: 'Détails du mouvement de dossier',
					height: 240,
					width: 900,
					modal: true,
					buttons: {
						Ok: function() {
							dialog.dialog("close");
						}
					}
				});


			} else {
				alert("Le mouvement n'existe pas.");
			}
		}).error(Ajax.notifyErrorHandler("affichage des détails du mouvement"));
	},

	/**
	 * Récupère ou crée à la demande un élément div pour contenir une boîte de dialogue
	 */
	create_dialog_div: function(id) {
		var dialog = $('#' + id);
		if (!dialog.length) {
			dialog = $('<div id="' + id + '" style="display:hidden"><img src="' + App.curl('/images/loading.gif') + '"/></div>');
			dialog.appendTo('body');
		}
		else {
			dialog.html('<img src="'+ App.curl('/images/loading.gif"/>')); // on vide la boîte de dialogue de son contenu précédant
		}
		return dialog;
	}
};

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

		var url = App.curl('/fors/motifsOuverture.do?tiersId=') + numeroCtb + '&genreImpot=' + genreImpot;
		if (rattachement != null) {
			url += '&rattachement=' + rattachement;
		}

		// appels ajax pour mettre-à-jour les motifs d'ouverture
		$.get(url + '&' + new Date().getTime(), function(motifs) {
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

		var url = App.curl('/fors/motifsFermeture.do?tiersId=') + numeroCtb + '&genreImpot=' + genreImpot;
		if (rattachement != null) {
			url += '&rattachement=' + rattachement;
		}
		if (StringUtils.isNotBlank(motifFermeture)) {
			url += '&oldMotif=' + motifFermeture;
		}

		// appels ajax pour mettre-à-jour les motifs de fermeture
		$.get(url + '&' + new Date().getTime(), function(motifs) {
			var list = '<option></option>'; // dans le cas du motif de fermeture, on ajoute toujours une option vide
			for(var i = 0; i < motifs.length; ++i) {
				var motif = motifs[i];
				list += '<option value="' + motif.type + '"' + (motif.type == motifFermeture ? ' selected=true' : '') + '>' + StringUtils.escapeHTML(motif.label) + '</option>';
			}
			motifsFermetureSelect.html(list);
		}, 'json')
		.error(Ajax.popupErrorHandler);
	},

	updateDatesFermetureForDebiteur: function(datesFermetureSelect, idFor, dateFin, forDejaFerme) {
		var url = App.curl('/fors/debiteur/datesFermeture.do?forId=') + idFor;

		// appel ajax
		$.get(url + '&' + new Date().getTime(), function(dates) {
			var options;
			var selected = dateFin;
			if (!forDejaFerme) {
				options += '<option value=""' + (selected == null || selected === '' ? ' selected="true"' : "") + '/>';
			}
			var count = dates.length;
			for (var i = 0 ; i < count ; ++ i) {
				var date = dates[i];
				var str = RegDate.format(date);
				options += '<option value="' + str + '"' + (selected === str ? ' selected="true"' : '') + '>' + str + '</option>';
			}
			datesFermetureSelect.html(options);
		}, 'json')
		.error(Ajax.popupErrorHandler);
	},

	autoCompleteCommunesVD: function(textInput, noOfsInput, onChangeCallback) {
		Autocomplete.infra('communeVD', $(textInput), true, function(item) {
			$(noOfsInput).val(item ? item.id1 : null);
			if (onChangeCallback) {
				onChangeCallback(item);
			}
		});
	},

	autoCompleteCommunesHC: function(textInput, noOfsInput, onChangeCallback) {
		Autocomplete.infra('communeHC', $(textInput), true, function(item) {
			$(noOfsInput).val(item ? item.id1 : null);
			if (onChangeCallback) {
				onChangeCallback(item);
			}
		});
	},

	autoCompletePaysHS: function(textInput, noOfsInput, onChangeCallback) {
		Autocomplete.infra('etat', $(textInput), true, function(item) {
			$(noOfsInput).val(item ? item.id1 : null);
			if (onChangeCallback) {
				onChangeCallback(item);
			}
		});
	},

	/**
	 * Construit la représentation Html de la table qui contient le résultat de simulation d'une action sur un for fiscal.
	 */
	buildActionTableHtml: function(results) {

		/** @namespace results.exception */
		/** @namespace results.errors */
		/** @namespace results.actions */

		var table = '<table class="sync_actions" border="0" cellspacing="0"><tbody>';

		// header
		if (results.exception) {
			table += '<tr class="header"><td colspan="2">';
			table += 'Les erreurs de validation suivantes seront levées si vous confirmez les changements';
			table += '</tr>';
			table += '<tr class="exception">';
			table += '<td>' + StringUtils.escapeHTML(results.exception) + '</td>';
			table += '</tr>'
		}
		else if (results.errors && results.errors.length > 0) {
			table += '<tr class="header"><td colspan="2">';
			table += 'Les erreurs de validation suivantes seront levées si vous confirmez les changements';
			table += '</tr>';
			for (var i = 0; i < results.errors.length; ++i) {
				table += '<tr class="action">';
				table += '<td class="rowheader">»</td>';
				table += '<td class="error">' + StringUtils.escapeHTML(results.errors[i]) + '</td>';
				table += '</tr>'
			}
		}
		else {
			table += '<tr class="header"><td colspan="2">';
			table += 'Les actions suivantes seront exécutées si vous confirmez les changements';
			table += '</tr>';
			for (i = 0; i < results.actions.length; ++i) {
				table += '<tr class="action">';
				table += '<td class="rowheader">»</td>';
				table += '<td class="action">' + StringUtils.escapeHTML(results.actions[i]) + '</td>';
				table += '</tr>'
			}
		}

		table += '</tbody></table>';

		return table;
	}

};

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
			document.location = App.curl(quickSearchTarget) + id;
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

};

//===================================================

var Rapport = {
	/**
	* Affiche le taux d'activité seulement si le type d'activité est PRINCIPAL
	*/


	/*
	* Annuler un rapport
	*/
	annulerRapport: function(idRapport) {
		if(confirm('Voulez-vous vraiment annuler ce rapport entre tiers ?')) {
			Form.doPostBack("theForm", "annulerRapport", idRapport);
		}
	}
};

//===================================================

//noinspection JSUnusedGlobalSymbols
/**
 * Classe utilitaire pour la manipulation de données en relation avec les tiers
 */
var Tiers = {
	/**
	 * Charge une vignette contenant les informations résumées d'un tiers (= contenu identique au bandeau principal).
	 * @param div le div ou l'élément html qui doit contenir la vignette
	 * @param numero un numéro de tiers
	 * @param options une liste d'options pour l'affichage d'informations supplémentaires.
	 */
	loadVignette:function (div, numero, options) {

		/** @namespace options.titre */
		/** @namespace options.showAvatar */
		/** @namespace options.showLinks */

		options = options || {};
		var titre = options.titre || 'Caractéristiques du tiers';
		var showAvatar = options.showAvatar;
		var showLinks = options.showLinks;

		var url = App.curl('tiers/vignette-info.do?numero=') + numero +
			'&fillActions=' + (showLinks ? 'true' : 'false') +
			'&fillAdresses=true' +
			'&fillEnsemble=' + (showLinks ? 'true' : 'false') +
			'&fillRoles=true' +
			'&fillUrlVers=' + (showLinks ? 'true' : 'false') + '&' + new Date().getTime();

		// affiche une image de chargement
		div.css('position', 'relative');
		var loading = $('<div style="position:absolute; left:' + ((div.width() / 2) - 12) + 'px; top:' + ((div.height() / 2) - 12) + 'px">' +
			'<img src="' + App.curl('/images/loading.gif') + '"/></div>');
		div.prepend(loading);

		// récupère les informations du tiers
		$.getJSON(url, function(tiers) {

			/** @namespace tiers.accessDenied */
			/** @namespace tiers.nature */
			/** @namespace tiers.typeAutoriteFiscaleForPrincipal */

			var html = '';
			if (tiers && tiers.accessDenied) {
				// le tiers existe mais il est protégé
				html += '<fieldset class="error"><legend><span>Contribuable protégé</span></legend>';
				html += '<table cellspacing="0" cellpadding="0" border="0"><tbody><tr>';
				html += '<td style="padding: 1em;">' + StringUtils.escapeHTML(tiers.accessDenied) +'</td>';
				html += '<td width="130 px"><img class="iepngfix" src="/fiscalite/unireg/web/images/tiers/protege.png"></td></tr>';
				html += '</tbody></table></fieldset>';
			}
			else if (tiers) {
				// construit la vignette
				html += '<fieldset class="information"><legend><span>'+ StringUtils.escapeHTML(titre) + '</span></legend>';
				html += '<input name="debugNatureTiers" type="hidden" value="' + tiers.nature + '"/>';
				html += '<input name="debugTypeForPrincipalActif" type="hidden" value="' + tiers.typeAutoriteFiscaleForPrincipal + '"/>';
				if (showAvatar || showLinks) {
					html += '<table cellspacing="0" cellpadding="0" border="0"><tr><td>\n';
					html += Tiers._buildDescriptifTiers(tiers, showLinks);
					html += '</td><td width="130 px">\n';
					html += Tiers._buildImageTiers(tiers);
					html += '</td></tr></table>';
				}
				html +=	'</fieldset>';
			}
			else {
				// le tiers n'existe pas
				html += '<div class="ctbInconnu iepngfix"/>';
			}

			div.html(html);

		}, 'json')
		.error(Ajax.popupErrorHandler);
	},

	_buildDescriptifTiers : function(tiers, showLinks) {

		/** @namespace tiers.dateAnnulation */
		/** @namespace tiers.dateDesactivation */
		/** @namespace tiers.roleLigne1 */
		/** @namespace tiers.roleLigne2 */
		/** @namespace tiers.adresseEnvoiException */
		/** @namespace tiers.adresseEnvoi */
		/** @namespace tiers.adresseEnvoi.ligne1 */

		var html = '';

		html += '<table cellspacing="0" cellpadding="5" border="0" class="display_table">';
		if (tiers.dateAnnulation) {
			html += '<tr class="inactif"><td colspan="3" width="100%" style="text-align: center;">TIERS ANNULE</td></tr>\n';
		}
		else if (tiers.dateDesactivation) {
			html += '<tr class="inactif"><td colspan="3" width="100%" style="text-align: center;">TIERS DESACTIVE AU ' + RegDate.format(tiers.dateDesactivation) + '</center></td></tr>\n';
		}
		else {
			// Numéro de contribuable
			html += '<tr class="odd">';
			html += '<td width="25%" nowrap>N° de tiers&nbsp;:&nbsp;</td>';
			html += '<td width="50%">' + Tiers.formatNumero(tiers.numero);
			if (showLinks) {
				html += Link.consulterLog('Tiers', tiers.numero);
			}
			html += '</td>';
			if (!tiers.dateAnnulation && showLinks) {
				html += '<td width="25%">';
				html += Tiers._buildVers(tiers);
				html += '</td>';
			}
			else {
				html += '<td width="25%">&nbsp;</td>\n';
			}
			html += '</tr>\n';

			// Rôle
			html += '<tr class="even">';
			html += '<td width="25%">Rôle&nbsp;:</td>';
			html += '<td width="50%">' + StringUtils.escapeHTML(tiers.roleLigne1);
			if (tiers.roleLigne2) {
				html += '<br>' + StringUtils.escapeHTML(tiers.roleLigne2);
			}
			html += '</td>\n';

			// Actions
			if (showLinks) {
				if (!tiers.actions || tiers.actions.length == 0) {
					// pas d'action à afficher
					html += '<td width="25%">&nbsp;</td>';
				}
				else {
					html += '<td width="25%"><div style="float:right;margin-right:10px;text-align:right">';
					html += '<span>Actions : </span><select onchange="return App.executeAction($(this).val());">';
					html += '<option>---</option>';
					for(var i in tiers.actions) {
						//noinspection JSUnfilteredForInLoop
						var action = tiers.actions[i];
						html += '<option value="' + action.url + '">' + StringUtils.escapeHTML(action.label) + '</option>';
					}
					html += '</select></div></td>';
				}
			}
			else {
				html += '<td width="25%">&nbsp;</td>\n';
			}
			html += '</tr>\n';

			// Adresse envoi
			if (tiers.adresseEnvoi) {
				// 1ère ligne
				html += '<tr class="odd">';
				html += '<td width="25%">Adresse&nbsp;:</td>';
				html += '<td width="75%" colspan="2">' + StringUtils.escapeHTML(tiers.adresseEnvoi.ligne1) + '</td>';
				// lignes 2 à 6
				for (i = 2; i <= 6; ++i) {
					var line = tiers.adresseEnvoi['ligne' + i];
					if (StringUtils.isNotBlank(line)) {
						html += '<tr class="odd">';
						html += '<td width="25%">&nbsp;</td>';
						html += '<td width="75%" colspan="2">' + StringUtils.escapeHTML(line) + '</td>';
						html += '</tr>';
					}
				}
			}
			else {
				html += '<tr class="odd"><td width="25%">Adresse&nbsp;:</td>';
				html += '<td width="75%" colspan="2" class="error">Erreur ! L\'adresse d\'envoi n\'a pas pu être déterminée pour la raison suivante:</td></tr>';
				html += '<tr class="odd"><td width="25%">&nbsp;</td>';
				html += '<td width="75%" colspan="2" class="error">=&gt;&nbsp;' + StringUtils.escapeHTML(tiers.adresseEnvoiException) + '</td>';
				html += '<tr class="odd"><td width="25%">Adresse&nbsp;:</td>';
				html +=
					'<td  width="75%"  colspan="2" class="error">Si ce problème persiste, veuillez s\'il-vous-plaît contacter l\'administrateur pour lui communiquer le message ci-dessus. Merci.</td></tr>';
			}
		}
		html += '</table>';
		return html;
	},

	_buildVers : function(tiers) {

		/** @namespace tiers.urlsVers */

		var html = '<div style="float: right;margin-right: 10px">';
		html += '<span>Vers : </span>';

		html += '<select name="AppSelect" onchange="App.gotoExternalApp(this);">';
		html += '<option value="">---</option>';
		for (var i in tiers.urlsVers) {
			//noinspection JSUnfilteredForInLoop
			var uv = tiers.urlsVers[i];
			var url = App.curl('/redirect/') + uv.appName + '.do?id=' + tiers.numero;
			html += '<option value="' + url + '">' + StringUtils.escapeHTML(uv.label) + '</option>';
		}
		html += '</select></div>';
		return html;
	},

	_buildImageTiers : function(tiers) {
		/** @namespace tiers.typeAvatar */
		var image = this._getImageUrl(tiers.typeAvatar, false);
		return '<img class="iepngfix" src="' + App.curl(image) + '">';
	},

	_getImageUrl : function(typeAvatar, forLink) {
		return "/tiers/avatar.do?type=" + typeAvatar + (forLink ? "&link=true" : "") + "&url_memorize=false";
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
			s = '<a href="' + App.curl('tiers/visu.do?id=') + numero + '">' + this.formatNumero(numero) + '</a>';
		}
		return s;
	},

	/**
	 * Effectue la validation (appel asynchrone) sur le tiers dont le numéro est spécifié, et retourne la liste des erreurs et des warnings à travers le callback spécifié.
	 */
	validate: function(numero, callback) {
		$.getJSON(App.curl('validation/tiers.do?id=') + numero + '&' + new Date().getTime(), callback, 'json').error(Ajax.notifyErrorHandler("validation du tiers"));
	},

	/**
	 * Effectue la validation (appel asynchrone) sur le tiers spécifié et affiche les éventuelles erreurs/warnings dans le div spécifié.
	 */
	loadValidationMessages: function(numero, div) {
		this.validate(numero, function(results) {
			var html = '';
			/** @namespace results.warnings */
			if (results.errors || results.warnings) {
				html += '<table class="validation_error" cellspacing="0" cellpadding="0" border="0">';
				html += '<tr><td class="heading">Un ou plusieurs problèmes ont été détectés sur ce contribuable ';
				html += '<span id="val_script">(<a href="#" onclick="$(\'#val_errors\').show(); $(\'#val_script\').hide(); return false;">voir le détail</a>)</span></td></tr>';
				html += '<tr id="val_errors" style="display:none"><td class="details"><ul>';

				if (results.errors) {
					for (var i in results.errors) {
						//noinspection JSUnfilteredForInLoop
						html += '<li class="err">Erreur: ' + StringUtils.escapeHTML(results.errors[i]) +'</li>'
					}
				}
				if (results.warnings) {
					for (i in results.warnings) {
						//noinspection JSUnfilteredForInLoop
						html += '<li class="warn">Avertissement: ' + StringUtils.escapeHTML(results.warnings[i]) +'</li>'
					}
				}
				html += '</ul></td></tr></table>';
			}
			$(div).html(html);
		});
	}
};

//===================================================

var Link = {

	/**
	 * Génère le code html qui va bien pour afficher une icône de visualisation des logs d'un élément.
	 * @param nature la nature de l'entité
	 * @param id l'id de l'entité
	 * @return {String} le code html qui va bien.
	 */
	consulterLog:function (nature, id) {
		var onclick = "return Dialog.open_consulter_log('" + nature + "', " + id + ");";
		return '<a href="#" class="consult" title="Consultation des logs" onclick="' + onclick + '">&nbsp;</a>';
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

			for (var i = 1 ; i < len; i++) {

				var visible;
				if (!showHisto) {
					var x = tbl.rows[i].cells;
					if (numCol >= x.length) {
						// work-around parce que le tag <display:table> ajoute une ligne avec une *seule* colonne lorsque la table est vide
						// cette ligne est masquée par défaut, on ne fait donc rien
						continue;
					}
					else {
						visible = StringUtils.isBlank(x[numCol].innerHTML) && x[numCol].innerHTML.indexOf('strike') == -1 && !this.hasClassName(tbl.rows[i], 'strike');
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
			for (var i=1 ; i< len; i++) {
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

		var showall = $('#linkAll-'+tableId);
		var showReduce = $('#linkReduce-'+tableId);

		if (isAll){
			showall.hide();
			showReduce.show();
		}
		else {
			showall.show();
			showReduce.hide();
		}
	},

	/**
	 * En fonction de la valeur de la checkbox, affiche ou pas les lignes de la table
	 * @param tableId table
	 * @param elementId checkbox
	 * @param triggerClass class des éléments tr qui ne doivent être affichés que si la checkbox est checkée
	 */
	toggleRowsIsHistoFromClass: function(tableId, elementId, triggerClass) {

		var tbl = $('#' + tableId).get(0);
		if (tbl != null) {
			// hide/show what must be hidden/shown
			var showHisto = $('#' + elementId).attr('checked');
			var toggleScope = $('#' + tableId + " tr." + triggerClass);
			if (showHisto) {
				toggleScope.show();
			}
			else {
				toggleScope.hide();
			}

			// reset odd/even classes
			var rows = $('#' + tableId + ' tr:visible');
			rows.removeClass('odd');
			rows.removeClass('even');
			$('#' + tableId + ' tr:visible:even').addClass('even');
			$('#' + tableId + ' tr:visible:odd').addClass('odd');
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

			for (var i=1 ; i< len; i++){
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

	// ---------------------------------------------------------
	// toggleRowsIsHistoAccordingToColumn
	//
	// Displays the lines of a table according to some criteria:
	// - if the checkbox given by its ID is checked, show everything
	// - otherwise
	//      - if the 'strike' class is set on the tr, the line is hidden
	//      - if the tr contains a td with 'notShownIfNotEmpty' class
	//              - if the td is empty -> the line is shown
	//              - if the td contains some html code -> the line is hidden
	//      - otherwise, the line is shown
	// Enventually, visible lines undergo a new computation of their 'odd' or 'even' classes.
	//
	// Arguments:
	//    idTable           html id of the table
	//    idCheckbox        html id of the checkbox
	//
	toggleRowsIsHistoAccordingToColumn: function(idTable, idCheckbox) {

		var table = $('#' + idTable).get(0);
		if (table != null) {
			var showHisto = $('#' + idCheckbox).get(0).checked;
			var nbLines = table.rows.length;
			for (var index = 0 ; index < nbLines ; ++ index) {
				var line = table.rows[index];
				var visible;
				if (!showHisto) {
					if (this.hasClassName(line, 'strike')) {
						visible = false;
					}
					else {
						visible = true;
						var column = $(line).find('td.notShownIfNotEmpty');
						if (column.length > 0) {
							for (var colIndex = 0 ; colIndex < column.length ; ++ colIndex) {
								visible = StringUtils.isBlank(column[colIndex].innerHTML);
								if (!visible) {
									break;
								}
							}
						}
					}
				}
				else {
					visible = true;
				}

				line.style.display = (visible ? '' : 'none');
			}

			this.computeEvenOdd(table);
		}
	},

	computeEvenOdd: function(table) {
		var rows = $(table).find('tr:visible');
		rows.removeClass('odd');
		rows.removeClass('even');
		$(table).find('tr:visible:even').addClass('even');
		$(table).find('tr:visible:odd').addClass('odd');
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

		if ($(table).length == 0) {
			return;
		}

		var rows = $(table).prop('rows');
		var foundSomething = false; // vrai si une ligne au moins est affichée

		for (var i = 1; i < rows.length; i++) { // on ignore l'entête
			var line = rows[i];
			if (line.className == 'empty') {
				// la table est vide, inutile d'aller plus loin
				return;
			}
			var dateFin = line.cells[dateFinIndex].innerHTML;
			var isHisto = (dateFin != null && StringUtils.isNotBlank(dateFin)); // date fin != null -> valeur historique
			var isBarre = line.className.indexOf('strike') >= 0;

			// affiche ou cache la ligne
			if (isHisto || isBarre) {
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
		}
		if (!showHisto && !foundSomething && rows.length > 1) { // si toutes les valeurs sont historiques, on affiche au minimum la plus récente
			rows[1].style.display = ''
		}
		Histo.computeEvenOdd(table);
	}
};

//===================================================

//noinspection JSUnusedGlobalSymbols
var StringUtils = {

	trim: function(string) {
		if (!string) {
			return '';
		}
		if (string.trim) {
			return string.trim();
		}
		else {
			return string.replace(/^\s+|\s+$/g, "");
		}
	},

	isBlank: function(s) {
		return this.trim(s).length == 0;
	},

	isNotBlank: function(s) {
		return !this.isBlank(s);
	},

	isEmptyString: function(str) {
		return (!str || 0 === str.length);
	},

	isBlankString: function(str) {
		return (!str || /^\s*$/.test(str));
	},

	escapeHTML: function(text) {
		return text ? $('<div/>').text(String(text)).html() : '';
	},

	leftpad: function (val, len, car) {
		val = String(val);
		len = len || 2;
		car = car || ' ';
		while (val.length < len) val = car + val;
		return val;
	},

	encodeURIComponent: function(str) {
		// la méthode javascript encodeURIComponent n'encode pas les caractères -_.!~*'() ... il faut donc le faire à sa place
		// voir http://stackoverflow.com/questions/75980/best-practice-escape-or-encodeuri-encodeuricomponent
		var jsEncoded = str ? encodeURIComponent(str) : '';
		return jsEncoded.replace(/\-/g, "%2D").replace(/_/g, "%5F").replace(/\./g, "%2E").replace(/!/g, "%21").replace(/~/g, "%7E").replace(/\*/g, "%2A").replace(/'/g, "%27").replace(/\(/g, "%28").replace(/\)/g, "%29");
	}
};

//===================================================

//noinspection JSUnusedGlobalSymbols
var DateUtils = {

	/*
	* Converti une chaine (dd.MM.yyyy) en date
	*/
	getDate: function(strDate, format){
		if (!strDate) {
			return null;
		}
		var day;
		var month;
		var year;
		if (format == 'dd.MM.yyyy') {
			day = parseInt(strDate.substring(0, 2));
			month = parseInt(strDate.substring(3, 5));
			year = parseInt(strDate.substring(6, 10));
		}
		else if (format == 'yyyy.MM.dd') {
			year = parseInt(strDate.substring(0, 4));
			month = parseInt(strDate.substring(5, 7));
			day = parseInt(strDate.substring(8, 10));
		}
		else {
			alert("Type de format inconnu !");
		}
		var d = new Date();
		d.setDate(day); // 1..31
		d.setMonth(month - 1); // 0..11
		d.setFullYear(year); // 4 digits
		return d;
	},

	/*
	* Converti une date au format spécifié (dd.MM.yyyy) en une date au format 'index'.
	*/
	toIndex: function(strDate, format){
		return this.toIndexString(this.getDate(strDate, format));
	},

	/*
	* Ajoute nombreAnnees années à la date
	*/
	addYear: function(strDate, nombreAnnees, format){
		var day;
		var month;
		var year;
		if (format == 'dd.MM.yyyy') {
			day = strDate.substring(0, 2);
			month = strDate.substring(3, 5);
			year = parseInt(strDate.substring(6, 10)) + nombreAnnees;
		}
		if (format == 'yyyy.MM.dd') {
			year = parseInt(strDate.substring(0,4)) + nombreAnnees;
			month = strDate.substring(5,7);
			day = strDate.substring(8,10);
		}
		var d = new Date();
		d.setDate(day);     // 1..31
		d.setMonth(month - 1);  // 0..11
		d.setFullYear(year);    // 4 digits
		return d;
	},

	/*
	 * Retourne:
	 *   0 si date_1=date_2
	 *   1 si date_1>date_2
	 *  -1 si date_1<date_2
	 */
	compare:function (date_1, date_2) {
		var diff = date_1.getTime() - date_2.getTime();
		return (diff == 0 ? diff : diff / Math.abs(diff));
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

	toIndexString: function(date) {
		if (!date) {
			return '';
		}

		return date.format('yyyymmdd');
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
			duration = days + 'j ' + hours + 'h ' + StringUtils.leftpad(minutes, 2, '0') + 'm ' + StringUtils.leftpad(seconds, 2, '0') + 's';
		}

		return duration;
	},

	/**
	 * Valide une date au format 'dd.MM.yyyy'
	 *
	 * Vérifie la correspondance à une date du calendrier grégorien et supporte les années bisextiles.
	 *
	 * @param date
	 * @returns {boolean}
	 */
	validate: function(date) {
		/*
		 Source: http://stackoverflow.com/a/31596630

		 Vérifie les dates du calendrier grégorien et supporte les années bisextiles.
		 */
		var dateVerificationPatterns =
			/^(((0[1-9]|[12][0-9]|30)[.]?(0[13-9]|1[012])|31[.]?(0[13578]|1[02])|(0[1-9]|1[0-9]|2[0-8])[.]?02)[.]?[0-9]{4}|29[.]?02[.]?([0-9]{2}(([2468][048]|[02468][48])|[13579][26])|([13579][26]|[02468][048]|0[0-9]|1[0-6])00))$/;

		return dateVerificationPatterns.test(date);
	}
};

var RegDate = {

	/**
	 * Converti une regdate (structure year/month/day) en date
	 * @param regdate
	 */
	toDate: function(regdate) {
		if (!regdate) {
			return null;
		}
		/** @namespace regdate.year */
		/** @namespace regdate.month */
		/** @namespace regdate.day */
		return new Date(regdate.year, regdate.month - 1, regdate.day);  // months are 0-based
	},

	format: function(regdate, format) {
		var date = this.toDate(regdate);
		if (!date) {
			return '';
		}
		format = format || 'shortDate';
		return date.format(format);
	}
};

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
		//noinspection UnnecessaryLocalVariableJS
		var dF = dateFormat;

		// You can't provide utc if you skip other args (use the "UTC:" mask prefix)
		if (arguments.length == 1 && Object.prototype.toString.call(date) == "[object String]" && !/\d/.test(date)) {
			mask = date;
			date = undefined;
		}

		// Passing date through Date applies Date.parse, if necessary
		date = date ? new Date(date) : new Date;
		if (isNaN(Number(date))) throw new SyntaxError("invalid date");

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

	contextPath : null,

	/**
	 * Initialise les librairies javascript de l'application. Cette méthode doit être appelée une seule fois par page.
	 * @param cp le context path de déploiement de l'application (e.g. http://localhost:8080/fiscalite/unireg/web)
	 */
	init: function(cp) {
		cp = cp.replace(/;jsessionid.*$/, ''); // supprime le jsession id qui apparaît de temps en temps dans IE...
		this.contextPath = cp;
		Ajax.init(); // appel immédiat pour catcher tous les appels ajax à partir de maintenant
	},

	/**
	 * Résoud une URL relative au context path en une URL absolue, sur le modèle du tag jsp <c:url>.
	 * @param url une URL relative
	 * @return une URL absolue pour le déploiement courant de la webapp.
	 */
	curl: function(url) {
		if (!url) {
			return '';
		}
		while (url.indexOf('/') == 0) {
			url = url.substring(1);
		}
		return this.contextPath + url;
	},

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
			var form = $('<form method="POST" action="' + App.curl(u) + '"/>');
			form.appendTo('body');
			form.submit();
		}
		else if (/^goto:/.test(url)) { // requête de type GOTO
			u = url.replace(/^goto:/, '');
			window.location.href = App.curl(u);
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
    },

	toggleBooleanParam: function(url, name, default_value){
		var regexp = new RegExp(name + "=([a-z]*)", "i");
		var match = regexp.exec(url);
		if (match == null) {
			// le paramètre n'existe pas, on l'ajoute
			var newUrl = new String(url);

			if (newUrl.charAt(newUrl.length - 1) == '#') { // supprime le trailing # si nécessaire
				newUrl = newUrl.substr(0, newUrl.length - 1);
			}
			return newUrl + '&' + name + '=' + default_value;
		}
		else {
			// le paramètre existe, on toggle sa valeur
			var oldvalue = (match[1] == 'true');
			var newvalue = !oldvalue;
			var param = name + "=" + newvalue;
			var newUrl = new String(url);
			newUrl = newUrl.replace(regexp, param);

			if (!newvalue) {
				// on recommence à la première page lorsqu'on passe de la liste complète à la liste partielle
				newUrl = newUrl.replace(/-p=[0-9]*/, "-p=1");
			}

			return newUrl;
		}
	}
};

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
			"\t Date : " + new Date() + "\n\n" +
			"\t URL : " + this.url + "\n\n" +
			"\t" + thrownError + ' (' +  xhr.status +') : '+ xhr.responseText);
    },

    /**
     * Error handler qui affiche le message d'erreur ajax dans une notification non-modale.
     */
    notifyErrorHandler: function(action) {
    	return function(xhr, ajaxOptions, thrownError) {
			$.jGrowl("Désolé ! Une erreur est survenue et l'action <span style=\"font-style: italic;\">" + action + "</span> n'a pas pu être effectuée.<br/><br/>" +
				"Veuillez réessayer plus tard, s'il-vous-plaît.<br/><br/>" +
				"Si le problème persiste, merci de communiquer à votre administrateur le message suivant :<br/><br/>" +
				"&nbsp;&nbsp;&nbsp;&nbsp;<span style=\"font-style: italic;\">" + StringUtils.escapeHTML(thrownError) + ' (' +  StringUtils.escapeHTML(xhr.status) +') : '+
				StringUtils.escapeHTML(xhr.responseText) + '</span>',
				{life:10000});
		}
    }
};


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
	},

	/**
	 * Crée une form dynamiquement et soumet-là.
	 *
	 * @param method la méthode de soumission de la form
	 * @param action l'action de la form
	 * @param params les paramètres (structure clé-valeur) de la soumission
	 */
	dynamicSubmit:function (method, action, params) {
		var html = '<form method="' + method + '" action="' + action + '">';
		if (params) {
			$.each(params, function (key, val) {
				html += '<input type=\"hidden\" name=\"' + key + '\" value=\"' + val + '\"/>';
			});
		}
		html += '</form>';
		var form = $(html);
		form.appendTo('body');
		form.submit();
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
			position: { my: "right top", at: "left bottom" },
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
			content: function() {
				// on détermine l'id de la div qui contient le tooltip à afficher
				var id = $(this).attr("id") + "-tooltip";
				id = id.replace(/\./g, '\\.'); // on escape les points

				// on récupère la div et on affiche son contenu
				var div = $("#" + id);
				return div.html();
			}
		});
	}
};

//===================================================

Object.extend = function(destination, source) {
	for (var property in source) {
		//noinspection JSUnfilteredForInLoop
		destination[property] = source[property];
	}
	return destination;
};

if (!window.Event) {
	var Event = new Object();
}

Object.extend(Event, {
	KEY_BACKSPACE: 8,
	KEY_TAB:       9,
	KEY_RETURN:   13,
	KEY_ESC:      27,
	KEY_LEFT:     37,
	KEY_UP:       38,
	KEY_RIGHT:    39,
	KEY_DOWN:     40,
	KEY_DELETE:   46,
	KEY_HOME:     36,
	KEY_END:      35,
	KEY_PAGEUP:   33,
	KEY_PAGEDOWN: 34,

	element: function(event) {
		return event.target || event.srcElement;
	},
	stop : function(event) {
		if (event.preventDefault) {
			event.preventDefault();
			event.stopPropagation();
		} else {
			event.returnValue = false;
			event.cancelBubble = true;
		}
		return false;
	}

});

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
		//noinspection UnnecessaryLocalVariableJS
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
	    var self = this;

	    $(theForm).submit(function(ev) {
			ev = ev || window.event;
			if (!self.submitSaveConfirmation()) {
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
						ev = ev || window.event;
						if (!self.submitResetConfirmation()) {
							return Event.stop(ev);
						}
					});
	            }
	        }
	        else if (tagName == "select") {
				$(element).change(function() {
					self.setIsModified( true);
				});
	        }
	        else if (tagName == "textarea") {
	            $(element).change(function() {
	             	self.setIsModified( true);
				});
	            $(element).keyup(function(event) {
					event = event || window.event;
					return self.onkeyup( event);
				});
	        }
	    }

	    var links = document.getElementsByTagName("A");
	    count = links.length;
	    for (i = 0; i < count; i++) {
	    	var link = links[i];
	    	if ( Modifier.isElementOver(link)) {
	    		var func = link.onclick;
			    link.onclick = function (ev) {
				    ev = ev || window.event;
				    if (!self.overConfirmation())
					    return Event.stop(ev);
				    //noinspection JSReferencingMutableVariableFromClosure
				    if (func) {
					    //noinspection JSReferencingMutableVariableFromClosure
					    func();
				    }
			    }
		    }
	    }

	    this.inputTarget = document.createElement("INPUT");
	    this.inputTarget.type = "hidden";
	    this.inputTarget.name = "__MODIFIER__";
	    theForm.appendChild( this.inputTarget);
	    this.setIsModified( this.isModifiedSending);
	},


	submitSaveConfirmation : function() {
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

	overConfirmation : function() {
	  	if ( this.isModified)
	  		return confirm(this.messageOverConfirmation);
	  	return true;
	},

	submitResetConfirmation : function() {
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

//noinspection JSUnusedGlobalSymbols
var Postit = {
	refresh : function() {
		$.get(App.curl('/postit/todo.do?') + new Date().getTime(), function(todo) {
			/** @namespace todo.taches */
			/** @namespace todo.dossiers */
			if (todo.taches > 0 || todo.dossiers > 0) {
				var text = 'Bonjour !<br>Il y a ';
				if (todo.taches > 0) {
					text += '<a href="' + App.curl('/tache/list.do')+ '">' + todo.taches + ' tâche(s)</a>';
				}
				if (todo.taches > 0 && todo.dossiers > 0) {
					text += ' et ';
				}
				if (todo.dossiers > 0) {
					text += '<a href="' + App.curl('/tache/list-nouveau-dossier.do')+ '">' + todo.dossiers + ' dossier(s)</a>';
				}
				text += ' en instance.';
				$('#postitText').html(text);
				$('#postit').show();
			}
			else {
				$('#postit').hide();
			}
		}, 'json');
	}
};

//===================================================

var Batch = {
	loadRunning: function(div, refreshInterval, readonly) {
		var requestDone = true;
		$(document).everyTime(refreshInterval, function() {
			if (!requestDone) {
				return;
			}
			requestDone = false;

			$.get(App.curl('/admin/batch/running.do?') + new Date().getTime(), function(jobs) {
				var h = Batch.__buildHtmlTableRunningBatches(jobs, readonly);
				$("#jobsActif").html(h);
				requestDone = true;
			}, 'json')
			.error(function(xhr, ajaxOptions, thrownError) {
				var message = '<span class="error">Oups ! Le chargement de la liste des batches en cours a provoqué l\'erreur suivante :' +
					'&nbsp;<span style="font-style: italic;">' + StringUtils.escapeHTML(thrownError) + ' (' +  StringUtils.escapeHTML(xhr.status) + ') : ' + StringUtils.escapeHTML(xhr.responseText) +
					'</span></span>';
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
		form.attr('action', App.curl('/admin/batch/start.do?name=') + StringUtils.encodeURIComponent(name));
		// cet appel nécessite la plugin jquery.form.js pour gérer l'upload ajax de fichiers dans les formulaires (voir http://malsup.com/jquery/form/)
		form.ajaxSubmit({
			success: function(responseText) {
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
		$.post(App.curl('/admin/batch/stop.do?name=') + StringUtils.encodeURIComponent(name), function(returnCode) {
			if (returnCode) {
				alert(returnCode);
			}
		});
	},

	__buildHtmlTableRunningBatches: function(jobs, readonly) {

		/** @namespace job.runningParams */
		/** @namespace job.lastEnd */
		/** @namespace job.lastStart */
		/** @namespace job.percentProgression */
		/** @namespace job.runningMessage */
		/** @namespace job.description */

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
				var onclick = "return Batch.stop('" + job.name + "');";
				table += '<td><a class="stop iepngfix" href="#" onclick="' + onclick + '"></a></td>';
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
				for(var key in job.runningParams) {
					//noinspection JSUnfilteredForInLoop
					var value = job.runningParams[key];
					//noinspection JSUnfilteredForInLoop
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
};

//===================================================

//noinspection JSUnusedGlobalSymbols
var DisplayTable = {

	buildPagination:function (page, pageSize, totalCount, buildGotoPageStatement) {

		var html = '';

		if ((totalCount > pageSize) && pageSize > 0) {
			html += '<table class="pageheader" style="margin-top: 0;"><tr>\n';
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
					html += '<span style="font-size: larger; "><strong>' + i + '</strong></font>&nbsp;\n';
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
			html += '<table class="pageheader" style="margin-top: 0;"><tr>\n';
			html += '<td class="pagebanner">Aucun élément trouvé.</td>';
			html += '</td></tr></table>';
		}

		return html;
	}
};

//===================================================

//noinspection JSUnusedGlobalSymbols
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

		$.get(App.curl("/admin/inbox/unreadSize.do?") + new Date().getTime(), function(unreadSize) {
			if (unreadSize > 0) {
				$(span).text(text + ' (' + unreadSize + ')');
				$(span).attr('style', 'font-weight: bold');
			}
			else {
				$(span).text(text);
				$(span).attr('style', '');
			}
			Inbox.requestInboxSizeDone = true;
		}, 'json');
	}
};

//===================================================

var Annonce = {

	/**
	 * Cette méthode ouvre un fenêtre popup avec les détails (read-only) de l'annonce dont l'id est passé en paramètre.
	 * @param id l'id de l'annonce
	 * @param userId l'identifiant IAM de l'utilisateur ou l'identifiant de Unireg dans RCEnt
	 */
	open_details: function(id, userId) {

		var dialog = Dialog.create_dialog_div('visu-annonce-dialog');

		// charge le contenu de la boîte de dialogue
		dialog.load(App.curl('/annonceIDE/visu.do?id='+ id + '&userId=' + userId + '&' + new Date().getTime()));

		dialog.dialog({
			title: "Détails de l'annonce n°" + id,
			height: 600,
			width: 800,
			modal: true,
			buttons: {
				Ok: function() {
					dialog.dialog("close");
				}
			}
		});
	}
};

var EvtOrg = {

	/**
	 * Conserve l'id de l'événement suivant lors de la dernière ouverture de détail via open_details() ci-dessous.
	 */
	nextId: null,

	/**
	 * Cette méthode ouvre un fenêtre popup avec les détails (read-only) de l'annonce dont l'id est passé en paramètre.
	 * @param idEvt l'id de l'événement organisation
	 * @param precedentEvtId l'id de l'événement organisation précédent dans la liste
	 * @param suivantEvtId l'id de l'événement organisation suivant dans la liste
	 */
	open_details: function(idEvt, precedentEvtId, suivantEvtId) {

		var dialog = Dialog.create_dialog_div('visu-evt-org-dialog');

		// charge le contenu de la boîte de dialogue
		var nextIdParam = suivantEvtId != null ? "&nextId=" + suivantEvtId : "";
		dialog.load(App.curl('/evenement/organisation/detail.do?id='+ idEvt + nextIdParam));

		var b = {
		};
		b["Ok"] = function() {
			dialog.dialog("close");
		};
		if (precedentEvtId) {
			b["Précédent"] = function() {
				if (precedentEvtId) {
					open_details(precedentEvtId);
				}
			};
		}
		if (suivantEvtId) {
			b["Suivant"] = function() {
				if (suivantEvtId) {
					open_details(suivantEvtId);
				}
			}
		}

		dialog.dialog({
			              title: "Détails de l'événement n°" + idEvt,
			              height: 800,
			              width: 900,
			              modal: true,
			              buttons: b
		              });
	}
};

var EvtCivil = {

	/**
	 * Conserve l'id de l'événement suivant lors de la dernière ouverture de détail via open_details() ci-dessous.
	 */
	nextId: null,

	/**
	 * Cette méthode ouvre un fenêtre popup avec les détails (read-only) de l'annonce dont l'id est passé en paramètre.
	 * @param idEvt l'id de l'événement organisation
	 * @param precedentEvtId l'id de l'événement organisation précédent dans la liste
	 * @param suivantEvtId l'id de l'événement organisation suivant dans la liste
	 */
	open_details: function(idEvt, precedentEvtId, suivantEvtId) {

		var dialog = Dialog.create_dialog_div('visu-evt-ech-dialog');

		// charge le contenu de la boîte de dialogue
		var nextIdParam = suivantEvtId != null ? "&nextId=" + suivantEvtId : "";
		dialog.load(App.curl('/evenement/ech/detail.do?id='+ idEvt + nextIdParam));

		var b = {
		};
		b["Ok"] = function() {
			dialog.dialog("close");
		};
		if (precedentEvtId) {
			b["Précédent"] = function() {
				if (precedentEvtId) {
					open_details(precedentEvtId);
				}
			};
		}
		if (suivantEvtId) {
			b["Suivant"] = function() {
				if (suivantEvtId) {
					open_details(suivantEvtId);
				}
			}
		}

		dialog.dialog({
			              title: "Détails de l'événement n°" + idEvt,
			              height: 800,
			              width: 900,
			              modal: true,
			              buttons: b
		              });
	}
};

//===================================================

var Decl = {
	/**
	 * Cette méthode ouvre un fenêtre popup avec les détails (read-only) de la déclaration d'impôt ordinaire (DI) dont l'id est passé en paramètre.
	 * @param diId l'id de la déclaration à afficher.
	 * @param pp <code>false</code> si les délais doivent être affichés sous leur forme simple (= commes pour les PP, sans état...), <code>true</code> sinon (= avec états, pour les PM)
	 */
	open_details_di: function(diId, pp) {

		$.getJSON(App.curl("/di/details.do?id=") + diId + "&" + new Date().getTime(), function(di) {
			/** @namespace di.delais */
			/** @namespace di.etats */
			/** @namespace di.periodeFiscale */
			/** @namespace di.codeControle */
			/** @namespace di.dateDebut */
			/** @namespace di.dateFin */
			/** @namespace di.typeDocumentMessage */
			if (di) {
				var info = '<fieldset class="information"><legend><span>Caractéristiques de la déclaration d\'impôt</span></legend>';
				info += '<table><tr class="odd"><td width="25%">Période fiscale&nbsp;:</td><td width="25%">' + di.periodeFiscale + '</td>';
				info += '<td width="25%">Code contrôle&nbsp;:</td><td width="25%">' + StringUtils.escapeHTML(di.codeControle) + '</td></tr>';
				info += '<tr class="even"><td width="25%">Début période imposition&nbsp;:</td><td width="25%">' + RegDate.format(di.dateDebut) + '</td>';
				info += '<td width="25%">Fin période imposition&nbsp;:</td><td width="25%">' + RegDate.format(di.dateFin) + '</td></tr>';
				info += '<tr class="odd"><td width="25%">Type déclaration&nbsp;:</td><td width="25%">' + StringUtils.escapeHTML(di.typeDocumentMessage) + '</td>';
				info += '<td width="25%">&nbsp;</td><td width="25%">&nbsp;</td></tr></table></fieldset>\n';

				var delais = pp ? Decl._buildDelaisPPISTable(di.delais) :  Decl._buildDelaisPMTable(di.delais);
				var etats = Decl._buidlEtatsTable(di.etats, false);

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

		$.getJSON(App.curl("/lr/details.do?id=") + lrId + "&" + new Date().getTime(), function(lr) {
			if (lr) {
				var info = '<fieldset class="information"><legend><span>Caractéristiques de la liste récapitulative</span></legend>';
				info += '<table><tr class="odd"><td width="50%">Date début période&nbsp;:</td><td width="50%">' + RegDate.format(lr.dateDebut) + '</td></tr>';
				info += '<tr class="even"><td width="50%">Date fin période&nbsp;:</td><td width="50%">' + RegDate.format(lr.dateFin) + '</td></tr></table></fieldset>\n';

				var delais = Decl._buildDelaisPPISTable(lr.delais);
				var etats = Decl._buidlEtatsTable(lr.etats, true);

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

	/**
	 * Cette méthode ouvre une fenêtre popup avec les détails (read-only) du questionnaire SNC dont l'ID est passé en paramètre
	 * @param id l'identifiant du questionnaire à afficher
	 */
	open_details_qsnc: function(id) {

		$.getJSON(App.curl("/qsnc/details.do?id=") + id + "&" + new Date().getTime(), function(qsnc) {
			if (qsnc) {
				var info = '<fieldset class="information"><legend><span>Caractéristiques du questionnaire SNC</span></legend>';
				info += '<table><tr class="odd"><td width="50%">Date début période&nbsp;:</td><td width="50%">' + RegDate.format(qsnc.dateDebut) + '</td></tr>';
				info += '<tr class="even"><td width="50%">Date fin période&nbsp;:</td><td width="50%">' + RegDate.format(qsnc.dateFin) + '</td></tr></table></fieldset>\n';

				var delais = Decl._buildDelaisPPISTable(qsnc.delais);
				var etats = Decl._buidlEtatsTable(qsnc.etats, true);

				var dialog = Dialog.create_dialog_div('details-di-dialog');
				dialog.html(info + delais + etats);

				dialog.dialog({
					              title: "Détails du questionnaire SNC",
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
				alert("Le questionnaire n'existe pas.");
			}
		})
		.error(Ajax.notifyErrorHandler("affichage des détails du questionnaire SNC"));
	},

	_buildDelaisPPISTable: function (delais) {
		var html = '';
		if (delais) {
			html = '<fieldset><legend><span>Délais</span></legend>';
			html +=
				'<table id="delai" class="display"><thead><tr><th>Date demande</th><th>Délai accordé</th><th>Confirmation écrite</th><th>Date traitement</th><th></th></tr></thead><tbody>';
			for (var i in delais) {
				//noinspection JSUnfilteredForInLoop
				var d = delais[i];
				/** @namespace d.delaiAccordeAu */
				/** @namespace d.dateDemande */
				/** @namespace d.confirmationEcrite */
				/** @namespace d.dateTraitement */
				html += '<tr class="' + (i % 2 == 0 ? 'even' : 'odd') + (d.annule ? ' strike' : '') + '">';
				html += '<td>' + RegDate.format(d.dateDemande) + '</td><td>' + RegDate.format(d.delaiAccordeAu) + '</td>';
				html += '<td>';
				if (d.confirmationEcrite) {
					html += '<input type="checkbox" checked="checked" disabled="disabled">';
					html += '<a href="' + App.curl('/declaration/copie-conforme-delai.do?idDelai=') + d.id + '&url_memorize=false" class="pdf" id="print-delai-' + d.id +
						'" onclick="Link.tempSwap(this, \'#disabled-print-delai-' + d.id + '\');">&nbsp;</a>';
					html += '<span class="pdf-grayed" id="disabled-print-delai-' + d.id + '" style="display:none;">&nbsp;</span>';
				}
				else {
					html += '<input type="checkbox" disabled="disabled">';
				}
				html += '</td>';
				html += '<td>' + RegDate.format(d.dateTraitement) + '</td>';
				html += '<td>' + Link.consulterLog('DelaiDeclaration', d.id) + '</td></tr>';
			}
			html += '</tbody></table></fieldset>\n';
		}
		return html;
	},

	_buildDelaisPMTable: function (delais) {
		var html = '';
		if (delais) {
			html = '<fieldset><legend><span>Délais</span></legend>';
			html +=
				'<table id="delai" class="display"><thead><tr><th>Date demande</th><th>Date traitement</th><th>Décision</th><th>Confirmation écrite</th><th>Délai accordé</th><th></th></tr></thead><tbody>';
			for (var i in delais) {
				//noinspection JSUnfilteredForInLoop
				var d = delais[i];
				/** @namespace d.delaiAccordeAu */
				/** @namespace d.dateDemande */
				/** @namespace d.confirmationEcrite */
				/** @namespace d.dateTraitement */
				/** @namespace d.etat */
				html += '<tr class="' + (i % 2 == 0 ? 'even' : 'odd') + (d.annule ? ' strike' : '') + '">';
				html += '<td>' + RegDate.format(d.dateDemande) + '</td><td>' + RegDate.format(d.dateTraitement) + '</td>';
				html += '<td>' + StringUtils.escapeHTML(d.etatMessage) + '</td>';
				html += '<td>';
				if (d.etat != 'DEMANDE') {
					if (d.confirmationEcrite) {
						html += '<a href="' + App.curl('/declaration/copie-conforme-delai.do?idDelai=') + d.id + '&url_memorize=false" class="pdf" id="print-delai-' + d.id +
							'" onclick="Link.tempSwap(this, \'#disabled-print-delai-' + d.id + '\');">&nbsp;</a>';
						html += '<span class="pdf-grayed" id="disabled-print-delai-' + d.id + '" style="display:none;">&nbsp;</span>';
					}
				}
				html += '</td>';
				if (d.sursis) {
					html += '<td>' + RegDate.format(d.delaiAccordeAu) + ' (Sursis)</td>';
				}
				else {
					html += '<td>' + RegDate.format(d.delaiAccordeAu) + '</td>';
				}
				html += '<td>' + Link.consulterLog('DelaiDeclaration', d.id) + '</td></tr>';
			}
			html += '</tbody></table></fieldset>\n';
		}
		return html;
	},

	_buidlEtatsTable: function (etats, lr) {
		var html = '';
		if (etats) {
			html = '<fieldset><legend><span>Etats</span></legend>';
			html += '<table id="etat" class="display"><thead><tr><th>Date</th><th>Etat</th>';
			if (!lr) { // SIFISC-6593 - On n'affiche pas la colonne source pour les LR
				html += '<th>Source</th>';
			}
			html += '<th></th></tr></thead><tbody>';
			for (var i in etats) {
				//noinspection JSUnfilteredForInLoop
				var e = etats[i];
				/** @namespace e.annule */
				/** @namespace e.dateObtention */
				/** @namespace e.dateEnvoiCourrierMessage */
				/** @namespace e.etatMessage */
				/** @namespace e.sourceMessage */
				/** @namespace e.etat */
				html += '<tr class="' + (i % 2 == 0 ? 'even' : 'odd') + (e.annule ? ' strike' : '') + '">';
				html += '<td>' + RegDate.format(e.dateObtention);
				if (!e.annule && (e.etat == 'SOMMEE' || e.etat == 'RAPPELEE')) {
					html += '&nbsp;(' + StringUtils.escapeHTML(e.dateEnvoiCourrierMessage) + ')';
				}
				html += '</td><td>' + StringUtils.escapeHTML(e.etatMessage);
				if (!e.annule && (e.etat == 'SOMMEE' || e.etat == 'RAPPELEE')) {
					var url;
					if (e.etat == 'RAPPELEE') {
						url = App.curl('/declaration/copie-conforme-rappel.do?idEtat=') + e.id;
					}
					else {
						url = App.curl('/declaration/copie-conforme-sommation.do?idEtat=') + e.id;
					}

					html += '&nbsp;' + '<a href="' + url + '&url_memorize=false" class="pdf" id="copie-sommation-' + e.id +
						'" onclick="Link.tempSwap(this, \'#disabled-copie-sommation-' + e.id + '\');">&nbsp;</a>';
					html += '<span class="pdf-grayed" id="disabled-copie-sommation-' + e.id + '" style="display:none;">&nbsp;</span>';
				}
				html += '</td>';
				if (!lr) { // SIFISC-6593 - On n'affiche pas la colonne source pour les LR
					html += '<td>';
					if (e.etat == 'RETOURNEE') {
						html += StringUtils.escapeHTML(e.sourceMessage);
					}
					html += '</td>';
				}
				html += '<td>' + Link.consulterLog('EtatDeclaration', e.id) + '</td></tr>';
			}
		}
		return html;
	}
};

//===================================================

var Search = {

	urlRetour:null,

	init: function(urlRetour, simpleQueryDefaultValue) {
		this.urlRetour = urlRetour;
		this._initSimple(simpleQueryDefaultValue);
		this._initMode();
	},

	_initMode: function () {
		// sélection du mode de recherche
		if ($.cookie("search-mode") == 'simple') {
			$('#simple-search').show();
			$('#advanced-search').hide();

			// fallback autofocus pour les browsers qui ne le supportent pas
			if (!("autofocus" in document.createElement("input"))) {
				$('#simple-search-input').focus();
			}
		}
	},

	/**
	 * Installe la fonction qui sera appelée à chaque frappe de touche pour la recherche simple
	 *
	 * @param defaultValue la valeur par défaut de la recherche
	 */
	_initSimple: function(defaultValue) {

		defaultValue = defaultValue || '';
		$('#simple-search-input').val(defaultValue);
		Search.executeSimpleQuery(defaultValue);

		var last = defaultValue;

		$('#simple-search-input').keyup(function() {
			var current = StringUtils.trim($('#simple-search-input').val());
			if (last != current) { // on ne rafraîchit que si le texte a vraiment changé
				last = current;

				// on retarde l'appel javascript de 250ms pour éviter de faire plusieurs requêtes lorsque l'utilisateur entre plusieurs caractères rapidemment
				clearTimeout($.data(this, "simple-search-timer"));
				var timer = setTimeout(function() {
					Search.executeSimpleQuery(current);
				}, 250); // 250 ms
				$.data(this, "simple-search-timer", timer);
			}
		});
	},

	/**
	 * Met-à-jour les résultats en fonction des critères spécifiés.
	 *
	 * @param query les critères de recherche.
	 */
	executeSimpleQuery: function(query) {
		var queryString = App.curl('/search/quick.do?query=' + StringUtils.encodeURIComponent(query) + '&saveQueryTo=simpleSearchQuery&' + new Date().getTime());

		// on effectue la recherche par ajax
		$.get(queryString, function(results) {
			$('#simple-search-results').html(Search._build_html_simple_results(results));
		}, 'json')
			.error(function(xhr, ajaxOptions, thrownError){
				var message = '<span class="error">Oups ! La recherche a provoqué l\'erreur suivante :' +
					'&nbsp;<span style="font-style: italic;">' + StringUtils.escapeHTML(thrownError) + ' (' +  StringUtils.escapeHTML(xhr.status) + ') : ' +
					StringUtils.escapeHTML(xhr.responseText) + '</span></span>';
				$('#simple-search-results').html(message);
			});
	},

	refreshSimpleSearch: function() {
		var query = StringUtils.trim($('#simple-search-input').val());
		Search.executeSimpleQuery(query);
	},

	toogleMode: function() {
		$('#simple-search').toggle();
		$('#advanced-search').toggle();
		$('#simple-search-results').html('');

		if ($('#simple-search').is(':visible')) {
			$.cookie("search-mode", "simple", { expires: 31 });
			$('#simple-search-input').focus();
			Search.refreshSimpleSearch();
		}
		else {
			$.cookie("search-mode", "advanced", { expires: 31 });
		}
		return false;
	},

	_build_html_simple_results: function(results) {
		var table = results.summary;

		if (results.entries.length > 0) {
			table += '<table border="0" cellspacing="0">';
			table += '<thead><tr class="header">';
			table += '<th>N° de tiers</th>' +
				'<th/>' +
				'<th nowrap>Rôle</th>' +
				'<th>Nom / Raison sociale</th>' +
				'<th>Date de naissance / d\'inscr. RC</th>' +
				'<th>NPA</th>' +
				'<th>Localité / Pays</th>' +
				'<th>For principal</th>' +
				'<th>Date ouv. 1er for VD</th>' +
				'<th>Date ferm. dernier for VD</th>' +
				'<th>Vers</th>';
			table += '</tr></thead>';
			table += '<tbody>';
			for(var i = 0; i < results.entries.length; ++i) {
				var e = results.entries[i];
				/** @namespace e.role2 */
				/** @namespace e.role1 */
				/** @namespace e.dateOuvertureVD */
				/** @namespace e.dateFermetureVD */

				table += '<tr class="' + (i % 2 == 1 ? 'even' : 'odd') + (e.annule ? ' strike' : '') + '">';
				table += '<td><a href="' + App.curl('/tiers/visu.do?id=' + e.numero) + '">' + Tiers.formatNumero(e.numero) + '</a></td>';
				if (e.typeAvatar != null) {
					table += '<td><img alt="" src="' + App.curl('/tiers/avatar.do?type=' + e.typeAvatar + '&url_memorize=false') + '" style="height: 2em;"/></td>';
				}
				else {
					table += '<td/>';
				}
				table += '<td nowrap>' + StringUtils.escapeHTML(e.role1) + (e.role2 ? '<br>' + StringUtils.escapeHTML(e.role2) : '' ) + '</td>';
				table += '<td>' + StringUtils.escapeHTML(e.nom1) + (e.nom2 ? '<br>' + StringUtils.escapeHTML(e.nom2) : '' ) + '</td>';
				table += '<td>' + StringUtils.escapeHTML(e.dateNaissance) + '</td>';
				table += '<td>' + StringUtils.escapeHTML(e.npa) + '</td>';
				table += '<td>' + StringUtils.escapeHTML(e.localitePays) + '</td>';
				table += '<td>' + StringUtils.escapeHTML(e.forPrincipal) + '</td>';
				table += '<td>' + RegDate.format(e.dateOuvertureVD) + '</td>';
				table += '<td>' + RegDate.format(e.dateFermetureVD) + '</td>';
				table += '<td>';
				if (!e.annule) {
					if (!Search.urlRetour) {
						var isPP = (e.tiersType === 'habitant' || e.tiersType === 'nonhabitant' || e.tiersType === 'menagecommun');

						table += '<select name="AppSelect" onchange="App.gotoExternalApp(this);">';
						table += '<option value="">---</option>';
						if (!e.debiteurInactif || isPP) {
							table += '<option value="' + App.curl('/redirect/TAO_PP.do?id=' + e.numero) + '">TAO-PP</option>';
						}
						if ((!e.debiteurInactif || isPP) && e.tiersType != 'entreprise') {
							table += '<option value="' + App.curl('/redirect/TAO_BA.do?id=' + e.numero) + '">TAO-BA</option>';
							var urlTaoIs = e.tiersType==='debiteurprestationimposable' ? '/redirect/TAO_IS_DEBITEUR.do?id=':'/redirect/TAO_IS.do?id=';
							table += '<option value="' + App.curl(urlTaoIs + e.numero) + '">TAO-IS</option>';
						}
						if (e.tiersType === 'entreprise') {
							table += '<option value="' + App.curl('/redirect/TAOPM.do?id=' + e.numero) + '">TAO-PM</option>';
						}
						table += '<option value="' + App.curl('/redirect/SIPF.do?id=' + e.numero) + '">SIPF</option>';
						if (isPP) {
							table += '<option value="' + App.curl('/redirect/DPERM.do?id=' + e.numero) + '">DPERM</option>';
						}
						if (e.tiersType != 'entreprise') {
							table += '<option value="' + App.curl('/tiers/launchcat.do?numero=' + e.numero) + '">CAT</option>';
						}
						table += '</select>';
					}
					else {
						table += '<a href="' + Search.urlRetour + StringUtils.escapeHTML(e.numero) + '" class="detail" title="Retour à l\'application appelante">&nbsp;</a>';
					}
				}
				table += '</td>';
			}
			table += '</tbody></table>';
		}

		return table;
	}
};

var gestionJMS = {

	/*
	 * Suspendre les messages
	 */
	confirmeArretQueue: function (identifiant,name) {
		if (confirm('Voulez-vous arrêter la queue '+name +' ?')) {
			var form = $("#formGestionQeues");
			form.attr('action', 'stop.do?id='+identifiant);
			form.submit();
		}
	},

	confirmeDemarrageQueue: function (identifiant,name) {
		if (confirm('Voulez-vous démarrer la queue '+name +' ?')) {
			var form = $("#formGestionQeues");
			form.attr('action', 'start.do?id='+identifiant);
			form.submit();
		}
	}



};


//===================================================

var Mandataires = {

	showDetailsMandat: function(idMandat) {
		$.getJSON(App.curl("/mandataire/adresse-de-mandat.do?idMandat=") + idMandat + "&" + new Date().getTime(), function(details) { Mandataires.showDetails(details); })
			.error(Ajax.notifyErrorHandler("affichage des détails du mandat"));
	},

	showDetailsAdresse: function(idAdresse) {
		$.getJSON(App.curl("/mandataire/adresse-mandataire.do?idAdresse=") + idAdresse + "&" + new Date().getTime(), function(details) { Mandataires.showDetails(details); })
			.error(Ajax.notifyErrorHandler("affichage des détails du mandat"));

	},

	showAdresseRepresentation: function(idTiers) {
		$.getJSON(App.curl("/mandataire/adresse-representation.do?idTiers=") + idTiers + "&" + new Date().getTime(), function(details) {
			if (details) {
				var info = '<fieldset class="information">';
				info += '<table><tr class="odd"><td>';
				var first = true;
				for (var idx in details.adresse) {
					var ligne = details.adresse[idx];
					if (StringUtils.isNotBlank(ligne)) {
						if (!first) {
							info += '<br/>';
						}
						info += StringUtils.escapeHTML(ligne);
						first = false;
					}
				}
				info += '</td></tr></table></fieldset>\n';
				Mandataires.openDialog('Adresse de représentation', info);
			}
			else {
				alert("Pas d'adresse de représentation trouvée.");
			}
		})
	},

	showDetails: function(details) {
		/** @namespace details.personneContact */
		/** @namespace details.noTelContact */
		/** @namespace details.adresse */
		/** @namespace details.erreur */
		if (details) {
			var info = '<fieldset class="information">';
			info += '<table><tr class="odd"><td width="40%">Personne de contact&nbsp;:</td><td>' + StringUtils.escapeHTML(details.personneContact) + '</td></tr>';
			info += '<tr class="even"><td width="40%">Téléphone de contact&nbsp;:</td><td>' + StringUtils.escapeHTML(details.noTelContact) + '</td></tr>';
			info += '<tr class="odd"><td width="40%">Adresse&nbsp;:</td><td>';
			if (details.erreur != null) {
				info += '<span class="erreur">' + StringUtils.escapeHTML(details.erreur) + '</span>';
			}
			else {
				var first = true;
				for (var idx in details.adresse) {
					var ligne = details.adresse[idx];
					if (StringUtils.isNotBlank(ligne)) {
						if (!first) {
							info += '<br/>';
						}
						info += StringUtils.escapeHTML(ligne);
						first = false;
					}
				}
			}
			info += '</td></tr>';
			info += '</table></fieldset>\n';

			Mandataires.openDialog('Détails du mandat', info);
		}
		else {
			alert("Le mandat n'existe pas.");
		}
	},

	openDialog: function(titre, htmlContent) {
		var dialog = Dialog.create_dialog_div('details-adresse-mandat-dialog');
		dialog.html(htmlContent);
		dialog.dialog({
			              title: titre,
			              width: 500,
			              modal: true,
			              buttons: {
				              Ok: function() {
					              dialog.dialog("close");
				              }
			              }
		              });
	}
};
