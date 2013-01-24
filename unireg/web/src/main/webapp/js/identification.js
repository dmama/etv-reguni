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
            form.attr('action', 'suspendre.do');
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
            form.attr('action', 'resoumettre.do');
            form.submit();
        }
    },

    confirmeDeblocageMessage: function() {
        if(confirm('Voulez-vous débloquer le(s) message(s) selectionné(s) ?')) {
            $('#desynchro').show();
            var form = $("#formRechercheMessage");
            form.attr('action', 'unlock.do');
            form.submit();
        }
    },

    confirmeBlocageMessage: function() {
        if(confirm('Voulez-vous débloquer le(s) message(s) selectionné(s) ?')) {
            $('#desynchro').show();
            var form = $("#formRechercheMessage");
            form.attr('action', 'lock.do');
            form.submit();
        }
    },

    effacerFormulaire:function (messageEnCours) {
        var form = $("#formRechercheMessage");
        if(messageEnCours){
            form.attr('action', 'effacerEnCours.do');
        }
        else{
            form.attr('action', 'effacerTraite.do');
        }

        form.submit();
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
            $("table#personne a.key").replaceWith('<span>&nbsp;</span>');
            Form.doPostBack("theForm", "identifier", idCtb);
        }
    }
};