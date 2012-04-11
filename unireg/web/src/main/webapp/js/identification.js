/**
* Selectionne / Deselectionne toutes les identifications
*/
function selectAllIdentifications(checkSelectAll) {

	var lignesMessage = document.getElementById('message').getElementsByTagName('tr');
	var taille = lignesMessage.length;

	for(var i=1; i < taille; i++) {
		$('#tabIdsMessages_' + i).attr('checked', checkSelectAll.checked);
	}
}

/*
* Suspendre les messages
*/
function confirmeSuspensionMessage() {
	if(confirm('Voulez-vous suspendre le(s) message(s) selectionné(s) ?')) {
		$('#desynchro').show();
		var form = $("#formRechercheMessage");
		form.attr('action', 'listEnCours.do?suspendre=suspendre');
		form.submit();
	}
}

/*
* Soumet les messages
*/
function confirmeSoumissionMessage() {
	if(confirm('Voulez-vous soumettre à nouveau le(s) message(s) selectionné(s) ?')) {
		$('#desynchro').show();
		var form = $("#formRechercheMessage");
		form.attr('action', 'listEnCours.do?soumettre=soumettre');
		form.submit();
	}
}

/*
* Expertise le message
*/
function confirmeExpertise(id) {
	if(confirm('Voulez-vous soumettre à expertise le message ?')) {
		var form = $("#formRecherchePersonne");
		form.attr('action', 'edit.do?id=' + id + '&expertiser=expertiser');
		form.submit();
	}
}

function Page_RetourNonIdentification() {
	if(confirm('Voulez-vous vraiment quitter cette page sans sauver ?')) {			
		window.location.href='edit.do';
	}
}

function page_NonIdentification( id ) {
	document.location.href='nonIdentifie.do?id='+id;
}

function voirMessage(id) {
	document.location.href='voirMessage.do?id='+id;
}

/*
* message impossible � identifier
*/
function confirmerImpossibleAIdentifier(id) {
	if(confirm('Voulez-vous marquer le message comme impossible à identifier ?')) {
		var form = $("#formNonIdentifie");
		form.attr('action', 'nonIdentifie.do');
		form.submit();		
	}
	
}

function Page_Identifier(idCtb) {
	if(confirm('Voulez-vous vraiment identifier ce message avec ce contribuable ?')) {
		Form.doPostBack("theForm", "identifier", idCtb);
	}
}
