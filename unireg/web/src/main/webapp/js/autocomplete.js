/**
 * Méthode pour activer l'autocompletion sur un champs de saisie texte. Les données utilisées pour l'autocompletion sont extraites du service d'infrastructure.
 *
 * @param category    		la catégorie de données utilisées comme autocompletion. Voir la classe java AutoCompleteInfraController pour les catégories supportées.
 * @param input     		le champ de saisie texte (ou son id) sur lequel l'autocompletion sera effective
 * @param validateSelection	active le validation de la selection, qui devient rouge si le texte saisi ne correspond à aucune valeur connue ([SIFISC-832])
 * @param on_change(item)   une function callback (optionnelle) appelée lorsqu'une valeur a été choisie par l'utilisateur
 */
function autocomplete_infra(category, input, validateSelection, on_change) {
	autocomplete("/autocomplete/infra.do?category=" + category, input, validateSelection, on_change);
}

/**
 * Méthode pour activer l'autocompletion sur un champs de saisie texte. Les données utilisées pour l'autocompletion sont extraites du service de sécurité.
 *
 * @param category    		la catégorie de données utilisées comme autocompletion. Voir la classe java AutoCompleteSecurityController pour les catégories supportées.
 * @param input     		le champ de saisie texte (ou son id) sur lequel l'autocompletion sera effective
 * @param validateSelection	active le validation de la selection, qui devient rouge si le texte saisi ne correspond à aucune valeur connue ([SIFISC-832])
 * @param on_change(item)   une function callback (optionnelle) appelée lorsqu'une valeur a été choisie par l'utilisateur
 */
function autocomplete_security(category, input, validateSelection, on_change) {
	autocomplete("/autocomplete/security.do?category=" + category, input, validateSelection, on_change);
}

/**
 * Méthode pour activer l'autocompletion sur un champs de saisie texte.
 *
 * @param url               l'url d'accès à la source des données d'autocompletion,
 * @param input     		le champ de saisie texte (ou son id) sur lequel l'autocompletion sera effective
 * @param validateSelection	active le validation de la selection, qui devient rouge si le texte saisi ne correspond à aucune valeur connue ([SIFISC-832])
 * @param on_change(item)   une function callback (optionnelle) appelée lorsqu'une valeur a été choisie par l'utilisateur
 */
function autocomplete(url, input, validateSelection, on_change) {

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
