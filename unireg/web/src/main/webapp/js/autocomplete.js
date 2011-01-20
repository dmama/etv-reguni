/**
 * Méthode pour activer l'autocompletion sur un champs de saisie texte. Les données utilisées pour l'autocompletion sont extraites du service d'infrastructure.
 *
 * @param category    		la catégorie de données utilisées comme autocompletion. Voir la classe java AutoCompleteInfraController pour les catégories supportées.
 * @param input     		le champ de saisie texte (ou son id) sur lequel l'autocompletion sera effective
 * @param on_change(item)   une function callback (optionnelle) appelée lorsqu'une valeur a été choisie par l'utilisateur
 */
function autocomplete_infra(category, input, on_change) {
	autocomplete("/autocomplete/infra.do?category=" + category, input, on_change);
}

/**
 * Méthode pour activer l'autocompletion sur un champs de saisie texte. Les données utilisées pour l'autocompletion sont extraites du service de sécurité.
 *
 * @param category    		la catégorie de données utilisées comme autocompletion. Voir la classe java AutoCompleteSecurityController pour les catégories supportées.
 * @param input     		le champ de saisie texte (ou son id) sur lequel l'autocompletion sera effective
 * @param on_change(item)   une function callback (optionnelle) appelée lorsqu'une valeur a été choisie par l'utilisateur
 */
function autocomplete_security(category, input, on_change) {
	autocomplete("/autocomplete/security.do?category=" + category, input, on_change);
}

/**
 * Méthode pour activer l'autocompletion sur un champs de saisie texte.
 *
 * @param url               l'url d'accès à la source des données d'autocompletion,
 * @param input     		le champ de saisie texte (ou son id) sur lequel l'autocompletion sera effective
 * @param on_change(item)   une function callback (optionnelle) appelée lorsqu'une valeur a été choisie par l'utilisateur
 */
function autocomplete(url, input, on_change) {

	var input = $(input);

	var is_open = false;
	var selected = null;

	input.autocomplete({
		source: getContextPath() + url,
		open: function(event, ui) {
			selected = null;
			is_open = true;
		},
		focus: function(event, ui) {
			// on met-à-jour la valeur affichée
			input.val(ui.item.label);
			return false;
		},
		select: function(event, ui) {
			// on mémorise la valeur sélectionnée
			selected = ui.item;
			return false;
		},
		close: function(event, ui) {
			is_open = false;
			// à la fermeture de l'auto-complete, on notifie de la valeur sélectionnée
			if (on_change) {
				on_change(selected);
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
		if (is_open) {
			// on ignore l'événement si le dialog d'autocompletion est ouverte, parce que cela veut dire que l'utilisateur a cliqué avec la souris sur un élément du menu
			return;
		}
		// si l'utilisateur a modifié le champ sans que l'autocompletion s'active, on force la notification en fin d'édition
		if (on_change) {
			on_change(selected);
		}
	});

	var previous = null;
	input.keyup(function(event) {
		var current = input.val();
		if (current != previous) {
			previous = current;
			// on met-à-null la valeur chaque fois que le texte de saisie change
			selected = null;
		}
	});
}
