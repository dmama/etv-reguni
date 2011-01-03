
/**
 * @return <b>true</b> si l'application courante est déployée en développement
 */
function is_dev_env() {
	var url = this.location.toString();
	// toutes urls sur les ports 7001 (weblogic) ou 8080 (tomcat) sont considérées comme "développement"
	return url.match(/http:\/\/[.\w]+:7001\//) || url.match(/http:\/\/[.\w]+:8080\//); 
}

/**
 * Demande la confirmation à l'utilisateur avant de détruire les données de la
 * base.
 * 
 * @return <b>true</b> si l'utilisateur veut continuer, <b>false</b> autrement.
 */
function confirm_trash_db() {
	if (is_dev_env()) {
		// A priori, un développeur sait ce qu'il fait...
		return true;
	}
	return confirm('Attention ! Cette opération va détruire les données existantes de la base.\n\nVoulez-vous vraiment continuer ?');
}

/**
 * Ouvre une boîte de dialogue modale qui permet de rechercher et de sélectionner un tiers.
 * <p>
 * Exemple d'utilisation:
 * <pre>
 *     <button onclick="return open_tiers_picker(this, function(id) {alert('le tiers n°' + id + ' a été sélectionné');});">...</button>
 * </pre>
 *
 * @param button              le button html sur lequel l'utilisateur a cliqué
 * @param on_tiers_selection  fonction de callback appelée avec le numéro de tiers sélectionné par l'utilisateur
 */
function open_tiers_picker(button, on_tiers_selection) {
	return open_tiers_picker_with_filter(button, null, null, on_tiers_selection);
}

/**
 * Ouvre une boîte de dialogue modale qui permet de rechercher et de sélectionner un tiers, le tout avec un filtre sur les résultats.
 * <p>
 * Exemple d'utilisation:
 * <pre>
 *     <button onclick="return open_tiers_picker_with_filter(this, 'tiersPickerFilterFactory', 'typeTiers:PERSONNE_PHYSIQUE', function(id) {alert('le tiers n°' + id + ' a été sélectionné');});">...</button>
 * </pre>
 *
 * @param button              le button html sur lequel l'utilisateur a cliqué
 * @param filter_bean		  le nom d'un bean spring qui implément l'interface TiersPickerFilterFactory
 * @param filter_params		  les paramètres du filter spécifié dans <i>filter_bean</i>
 * @param on_tiers_selection  fonction de callback appelée avec le numéro de tiers sélectionné par l'utilisateur
 */
function open_tiers_picker_with_filter(button, filter_bean, filter_params, on_tiers_selection) {

	// on récupère ou on crée à la demande le div de la boîte de dialogue
	var dialog = create_dialog_div('tiers-picker-dialog');

	// on définit la méthode appelée lorsque un tiers a été choisi
	button.select_tiers_id = function(link) {
		var tiersId = link.innerHTML.replace(/[^0-9]*/g, ''); // remove non-numeric chars
		dialog.dialog("close");
		if (on_tiers_selection) {
			on_tiers_selection(tiersId);
		}
	};

	// load remote content
	var url = getContextPath() + "tiers/picker/tiers-picker.do";
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
						'buttonId' : button.id
					};
					if (filter_bean) {
						params['filterBean'] = filter_bean;
						params['filterParams'] = filter_params;
					}
					XT.doAjaxAction('tiersPickerQuickSearch', document.getElementById('tiers-picker-query'), params);
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
				'buttonId' : button.id
			};
			if (filter_bean) {
				params['filterBean'] = filter_bean;
				params['filterParams'] = filter_params;
			}
			XT.doAjaxAction('tiersPickerFullSearch', document.getElementById('tiers-picker-query'), params);
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
}

/**
 * Ouvre une boîte de dialog modale qui affiches les dates de création/modification de l'élément spécifié.
 *
 * @param nature le type d'élément
 * @param id     l'id de l'élément
 */
function open_consulter_log(nature, id) {

	var dialog = create_dialog_div('consulter-log-dialog');

	// charge le contenu de la boîte de dialogue
	dialog.load(getContextPath() + '/common/consult-log.do?nature=' + nature + '&id=' + id);

	dialog.dialog({
		title: 'Consultation des logs',
		height: 200,
		width: 800,
		resizable: false, // TODO (msi) parce que le resizing ne fonctionne pas à cause de custom.js
		modal: true,
		buttons: {
			Ok: function() {
				dialog.dialog("close");
			}
		}
	});

	//prevent the browser to follow the link
	return false;
}

/**
 * Ouvre une boîte de dialog modale qui affiche les détails d'un mouvement de dossier
 *
 * @param id l'id du mouvement de dossier
 */
function open_details_mouvement(id) {

	var dialog = create_dialog_div('details-mouvement-dialog');

	// charge le contenu de la boîte de dialogue
	dialog.load(getContextPath() + '/tiers/mouvement.do?idMvt=' + id);

	dialog.dialog({
		title: 'Détails du mouvement de dossier',
		height: 440,
		width: 900,
		resizable: false, // TODO (msi) parce que le resizing ne fonctionne pas à cause de custom.js
		modal: true,
		buttons: {
			Ok: function() {
				dialog.dialog("close");
			}
		}
	});

	//prevent the browser to follow the link
	return false;
}

/**
 * Récupère ou crée à la demande un élément div pour contenir une boîte de dialogue
 */
function create_dialog_div(id) {
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
