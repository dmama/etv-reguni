/**
* Selectionne / Deselectionne toutes les identifications
*/
function selectAllIdentifications(checkSelectAll) {

	var lignesMessage = document.getElementById('message').getElementsByTagName('tr');
	var taille = lignesMessage.length;

	if (checkSelectAll.checked) {
		for(var i=1; i < taille; i++) {
			if (E$('tabIdsMessages_' + i) != null) {
				E$('tabIdsMessages_' + i).checked = true ;
			}
		}
	} else {
		for(var i=1; i < taille; i++) {
			if (E$('tabIdsMessages_' + i) != null) {
				E$('tabIdsMessages_' + i).checked = false ;
			}
		}
	}
}

/*
* Suspendre les messages
*/
function confirmeSuspensionMessage() {
	if(confirm('Voulez-vous suspendre le(s) message(s) selectionné(s) ?')) {
		$('#desynchro').show();
		var form = F$("theForm");
		form.action = 'listEnCours.do?suspendre=suspendre';
		form.submit();
	}
}

/*
* Soumet les messages
*/
function confirmeSoumissionMessage() {
	if(confirm('Voulez-vous soumettre à nouveau le(s) message(s) selectionné(s) ?')) {
		$('#desynchro').show();
		var form = F$("theForm");
		form.action = 'listEnCours.do?soumettre=soumettre';
		form.submit();
	}
}

/*
* Expertise le message
*/
function confirmeExpertise(id) {
	if(confirm('Voulez-vous soumettre à expertise le message ?')) {
		var form = F$("theForm");
		form.action = 'edit.do?id=' + id + '&expertiser=expertiser';
		form.submit();
	}
}

function Page_RetourNonIdentification() {
	if(confirm('Voulez-vous vraiment quitter cette page sans sauver ?')) {			
		
			window.location.href='edit.do';
		
		
	}
}

/*
* message impossible � identifier
*/
function confirmerImpossibleAIdentifier(id) {
	if(confirm('Voulez-vous marquer le message comme impossible à identifier ?')) {
		var form = F$("formNonIdentifie");
		form.action = 'nonIdentifie.do';
		form.submit();		
	}
	
}

function getParamValue( name )
{
  name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
  var regexS = "[\\?&]"+name+"=([^&#]*)";
  var regex = new RegExp( regexS );
  var results = regex.exec( window.location.href );
  if( results == null )
    return "";
  else
    return results[1];
}
