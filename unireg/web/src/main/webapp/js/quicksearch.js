/**
 * Détecte la pression de la touche 'enter' et navigue vers la page d'affichage
 * du contribuable dont le numéro a été saisi. Les caractères non-numériques
 * sont ignorés.
 */
function quickSearch(input, e) {
	var characterCode;

	if (e && e.which) {
		e = e;
		characterCode = e.which;
	} else {
		e = event;
		characterCode = e.keyCode;
	}

	if (characterCode == 13) {
		quickSearchShowCtb(input);
		return false;
	} else {
		return true;
	}
}

/**
 * Navigue vers la page d'affichage du contribuable dont le numéro a été spécifié
 */
function quickSearchShowCtb(input) {
	var value = new String(input.value);
	value = value.replace(/[^0-9]*/g, ''); // remove non-numeric chars
	var id = parseInt(value, 10);
	if (!isNaN(id)) {
		document.location = getContextPath() + "/tiers/visu.do?id=" + id;
	}
}

function quickSearchFocus(input) {
	input.value = "";
	input.style.color = "black";
}

function quickSearchBlur(input, invite) {
	input.value = invite;
	input.style.color = "gray";
}
