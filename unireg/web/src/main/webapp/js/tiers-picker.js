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

	// on récupère ou on crée à la demande le div de la boîte de dialogue
	var dialog = $('#tiers-picker-dialog');
	if (!dialog.length) {
		dialog = $('<div id="tiers-picker-dialog" style="display:hidden"></div>');
		dialog.appendTo('body');
	}
	else {
		dialog.attr('innerHTML', ''); // on vide la boîte de dialogue de son contenu précédant
	}

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
		// on installe la fonction qui sera appelée à chaque frappe de touche
		query.keyup(function() {
			var current = query.val();
			if (last != current) { // on ne rafraîchit que si le texte a vraiment changé
				last = current;
				XT.doAjaxAction('updateTiersPickerSearch', document.getElementById('tiers-picker-query'), {
					'query' : current,
					'buttonId' : button.id
				});
			}
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
