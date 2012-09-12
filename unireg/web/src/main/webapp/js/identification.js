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

    /*
    * Suspendre les messages
    */
    confirmeSuspensionMessage: function() {
        if(confirm('Voulez-vous suspendre le(s) message(s) selectionné(s) ?')) {
            $('#desynchro').show();
            var form = $("#formRechercheMessage");
            form.attr('action', 'listEnCours.do?suspendre=suspendre');
            form.submit();
        }
    },

    /*
    * Soumet les messages
    */
    confirmeSoumissionMessage: function() {
        if(confirm('Voulez-vous soumettre à nouveau le(s) message(s) selectionné(s) ?')) {
            $('#desynchro').show();
            var form = $("#formRechercheMessage");
            form.attr('action', 'listEnCours.do?soumettre=soumettre');
            form.submit();
        }
    },

    /*
     * message impossible à identifier
     */
    confirmerImpossibleAIdentifier: function(id) {
        if(confirm('Voulez-vous marquer le message comme impossible à identifier ?')) {
            var form = $("#formNonIdentifie");
            form.attr('action', 'nonIdentifie.do');
            form.submit();
        }
    },

    Page_Identifier: function(idCtb) {
        if(confirm('Voulez-vous vraiment identifier ce message avec ce contribuable ?')) {
            Form.doPostBack("theForm", "identifier", idCtb);
        }
    }
};