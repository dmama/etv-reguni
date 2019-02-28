'use strict';
var di = {'di': {}};

(function (di) {
	di.creerModalLiberationDI = creerModalLiberationDI;

	function creerModalLiberationDI(idDI, idModal, action) {

		var dialog = Dialog.create_dialog_div(idModal);

		// charge le contenu de la boîte de dialogue
		dialog.load(App.curl(action) + '?id=' + idDI + '&' + new Date().getTime());

		dialog.dialog({
			              title: "Libération de DI",
			              height: 220,
			              width: 500,
			              modal: true,
			              buttons: {
				              "Valider libération DI": function (event) {
					              // les boutons ne font pas partie de la boîte de dialogue (au niveau du DOM), on peut donc utiliser le sélecteur jQuery normal

					              var form = dialog.find('#formLiberation');
					              var motif = form.find('textarea[id=motifValue]').val();
					              if (motif === null || motif === "") {
						              alert('Veuillez préciser le motif de liberation de la DI');
					              }
					              else {
						              var buttons = $('.ui-button');
						              buttons.each(function () {
							              if ($(this).text() === 'Valider libération DI') {
								              $(this).addClass('ui-state-disabled');
								              $(this).attr('disabled', true);
							              }
						              });
						              form.attr('action', App.curl(action));
						              form.submit();
					              }

				              },
				              "Annuler": function () {
					              dialog.dialog("close");
				              }
			              }
		              });
	}
})(di);