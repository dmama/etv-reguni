var IdentificationCtb = {

    /**
    * Selectionne / Deselectionne toutes les identifications
    */
    selectAllIdentifications: function(checkSelectAll) {

        var lignesMessage = document.getElementById('message').getElementsByTagName('tr');
        var taille = lignesMessage.length;

        for(var i=1; i < taille; i++) {
            $('#tabIdsMessages_' + i).attr('checked', checkSelectAll.checked);
        }
    },

	isAtLeastOneMessageSelected: function() {
		var lignesMessage = document.getElementById('message').getElementsByTagName('tr');
		var taille = lignesMessage.length;

		for(var i=1; i < taille; i++) {
			var sel = $('#tabIdsMessages_' + i);
			if (sel && !sel.disabled && sel.attr('checked')) {
				return true;
			}
		}
		return false;
	},

	desactiveBoutonsActionsEnMasse: function() {
		$('#actions-masse-messages').find(':button').attr('disabled', true);
	},

	/*
	* Suspendre les messages
	*/
    confirmeSuspensionMessage: function() {
	    if (!this.isAtLeastOneMessageSelected()) {
		    alert("Veuillez sélectionner au moins un message à suspendre.");
	    }
        else if (confirm('Voulez-vous suspendre le(s) message(s) selectionné(s) ?')) {
		    this.desactiveBoutonsActionsEnMasse();
            var form = $("#formRechercheMessage");
            form.attr('action', 'suspendre.do');
            form.submit();
        }
    },

    /*
    * Soumet les messages
    */
    confirmeSoumissionMessage: function() {
	    if (!this.isAtLeastOneMessageSelected()) {
		    alert("Veuillez sélectionner au moins un message à soumettre.");
	    }
	    else if (confirm('Voulez-vous soumettre à nouveau le(s) message(s) selectionné(s) ?')) {
		    this.desactiveBoutonsActionsEnMasse();
            var form = $("#formRechercheMessage");
            form.attr('action', 'resoumettre.do');
            form.submit();
        }
    },

    confirmeDeblocageMessage: function() {
	    if (!this.isAtLeastOneMessageSelected()) {
		    alert("Veuillez sélectionner au moins un message à débloquer.");
	    }
	    else if (confirm('Voulez-vous débloquer le(s) message(s) selectionné(s) ?')) {
		    this.desactiveBoutonsActionsEnMasse();
            var form = $("#formRechercheMessage");
            form.attr('action', 'unlock.do');
            form.submit();
        }
    },

    confirmeBlocageMessage: function() {
	    if (!this.isAtLeastOneMessageSelected()) {
		    alert("Veuillez sélectionner au moins un message à bloquer.");
	    }
	    else if (confirm('Voulez-vous bloquer le(s) message(s) selectionné(s) ?')) {
		    this.desactiveBoutonsActionsEnMasse();
            var form = $("#formRechercheMessage");
            form.attr('action', 'lock.do');
            form.submit();
        }
    },

    effacerFormulaire:function (mySource) {
        var form = $("#formRechercheMessage");
        if(mySource == 'enCours'){
            form.attr('action', 'effacerEnCours.do');
        }
        else if(mySource == 'traite'){
            form.attr('action', 'effacerTraite.do');
        }
        else if(mySource == 'suspendu'){
            form.attr('action', 'effacerSuspendu.do');
        }

        form.submit();
    },

    /*
     * message impossible à identifier
     */
    confirmerImpossibleAIdentifier: function() {
        if(confirm('Voulez-vous marquer le message comme impossible à identifier ?')) {
            const form = $("#formNonIdentifie");
            form.submit();
        }
    },

    Page_Identifier: function(idCtb) {
        if(confirm('Voulez-vous vraiment identifier ce message avec ce contribuable ?')) {
            $("table#personne a.key").replaceWith('<span>&nbsp;</span>');
	        const input = $('#contribuableIdentifie')[0];
	        input.setAttribute('value', idCtb);
	        const form = $("#formIdentification");
	        form.submit();
        }
    }
};