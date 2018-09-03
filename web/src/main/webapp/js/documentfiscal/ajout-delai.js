'use strict';
var DelaiSNC = {'DelaiSNC': {}};

(function (DelaiSNC) {

	DelaiSNC.ajouterDelai = ajouterDelai;
	DelaiSNC.toggleDecision = toggleDecision;
	DelaiSNC.verifierDelai = verifierDelai;
	DelaiSNC.modifierDelai = modifierDelai;

	function ajouterDelai(button, type) {

		if ($('#decision').val() === 'ACCORDE' && !verifierDelai()) {
			return false;
		}

		$('#typeImpression').val(type);
		$('.error').hide();         // [SIFISC-18869] il faut enlever les éventuels messages d'erreur de l'affichage
		$(button).closest("form").submit();

		// On desactive les boutons
		$('#ajouter, #annuler, #envoi-auto, #envoi-manuel').hide();
		$('#retour').show();

		return true;
	}

	function verifierDelai() {
		var dateExpedition = '${command.dateExpedition}';
		var delaiAccordeAu = $('#delaiAccordeAu').val();
		if (DateUtils.validate(delaiAccordeAu) && DateUtils.compare(DateUtils.addYear(dateExpedition, 1, 'yyyy.MM.dd'), DateUtils.getDate(delaiAccordeAu, 'dd.MM.yyyy')) === -1) {
			return confirm("Ce délai est située plus d'un an dans le futur à compter de la date d'expédition de la DI. Voulez-vous le sauver ?");
		}
		return true;
	}

	function toggleDecision() {
		var decisionSelectionnee = $('#decision').val();
		$('.siDelaiAccorde, .siDelaiRefuse, #envoi-auto, #envoi-manuel, #ajouter').hide();
		if (decisionSelectionnee === 'ACCORDE') {
			$('.siDelaiAccorde').show();
			$('#envoi-auto, #envoi-manuel').show();
		}
		else if (decisionSelectionnee === 'REFUSE') {
			$('.siDelaiRefuse').show();
			$('#envoi-auto, #envoi-manuel').show();
		}
		else {
			$('#ajouter').show();
		}
	}

	function modifierDelai(button, type) {
		if ($('#decision').val() === 'ACCORDE' && !verifierDelai()) {
			return false;
		}

		$('#typeImpression').val(type);
		$(button).closest("form").submit();

		// On desactive les boutons
		$('#annuler, #envoi-auto, #envoi-manuel').hide();
		$('#retour').show();

		return true;
	}


}(DelaiSNC));