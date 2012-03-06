/*
* Supprime les espaces avant et après
*/
function trim(chaine) 
{ 
    if(trim.arguments.length > 1) 
    { 
        var str = trim.arguments[1]; 
        expreg = new RegExp('(^'+ str +'*)|('+ str +'*$)', 'g'); 
    } 
    else 
    { 
        expreg = /(^\s*)|(\s*$)/g; 
    } 
    return chaine.replace(expreg,''); 
} 

function isEmptyString(str) {
    return (!str || 0 === str.length);
}

function isBlankString(str) {
    return (!str || /^\s*$/.test(str));
}

function escapeHTML(text) {
	return text ? $('<div/>').text(text).html() : null;
}

/*
* Converti une chaine (dd.MM.yyyy) en date 
*/
function getDate(strDate, format){	
	if (format == 'dd.MM.yyyy') {  
		day = parseInt(strDate.substring(0,2));
		month = parseInt(strDate.substring(3,5));
		year = parseInt(strDate.substring(6,10));
	}
	else if (format == 'yyyy.MM.dd') {
		year = parseInt(strDate.substring(0,4));
		month = parseInt(strDate.substring(5,7));
		day = parseInt(strDate.substring(8,10));
	}
	else {
		alert("Type de format inconnu !");
	}
	d = new Date();
	d.setDate(day); // 1..31
	d.setMonth(month - 1); // 0..11
	d.setFullYear(year); // 4 digits
	return d;  
}

/*
* Ajoute nombreAnnees années à la date
*/
function addYear(strDate, nombreAnnees, format){
	if (format == 'dd.MM.yyyy') {  
		day = strDate.substring(0,2);
		month = strDate.substring(3,5);
		year = parseInt(strDate.substring(6,10)) + nombreAnnees;
	}
	if (format == 'yyyy.MM.dd') {  
		year = parseInt(strDate.substring(0,4)) + nombreAnnees;
		month = strDate.substring(5,7);
		day = strDate.substring(8,10);
	}
	d = new Date();
	d.setDate(day);
	d.setMonth(month);
	d.setFullYear(year); 
	return d; 
}

/*
* Retourne:
*   0 si date_1=date_2
  *   1 si date_1>date_2
*  -1 si date_1<date_2
*/	  
function compare(date_1, date_2){
  diff = date_1.getTime()-date_2.getTime();
  return (diff==0?diff:diff/Math.abs(diff));
}

/**
 * Ces trois méthodes sont utilisées pour soumettre une forme avec un nom d'action (eventTarget) et
 * un argument (eventArgument). La forme doit posséder deux champs cachés nommés __TARGET__ et
 * __EVENT_ARGUMENT__. Il s'agit donc d'un mécanisme bizarre qui ne devrait plus être utilisé dans
 * les nouveaux écrans.
 */
var Form = {
	doPostBack : function(formName, eventTarget, eventArgument) {
		var theForm = $("form[name='" + formName + "']");
		if (theForm.length > 0) {
			$('input[name="__TARGET__"]', theForm).val(eventTarget);
			$('input[name="__EVENT_ARGUMENT__"]', theForm).val(eventArgument);
	        theForm.submit();
	    }
	},
	doAjaxActionPostBack : function(formName, event, eventTarget, eventArgument) {
		eventTarget = $(eventTarget);
		var eventId = eventTarget.attr('name');
		if (!eventId) {
			eventId = eventTarget.attr('id');
		}
		eventArgument = (!eventArgument ? {}: eventArgument);
		XT.doAjaxAction(eventId + event, eventTarget, eventArgument, {
			clearQueryString: true,
			formName: formName
		});
	},
	doAjaxSubmitPostBack : function(formName, event, eventTarget, eventArgument) {
		eventTarget = $(eventTarget);
		var eventId = eventTarget.attr('name');
		if (!eventId) {
			eventId = eventTarget.attr('id');
		}
		eventArgument = (!eventArgument ? {}: eventArgument);
		XT.doAjaxSubmit(eventId + event, eventTarget, eventArgument, {
			clearQueryString: true,
			formName:formName
		});
	}
};

/**
 * Redirige la page courante vers l'application (TAO, SIPF, CAT, ...) désirée
 */
function AppSelect_OnChange(select) {
	var value = select.options[select.selectedIndex].value;
	if ( value && value !== '') {
		window.location.href = value;
	}
}

/**
 * Execute l'action spécifiée sous forme d'url.
 * <p>
 * Les formats d'URL suivants sont supportés:
 *   - "goto:/some/url/" : navige à l'url spécifiée
 *   - "post:/some/other/url?param=machin" : exécute une requête HTML de type POST
 */
function executeAction(url) {
	if (/^---/.test(url)) { // pattern de non-action -> rien à faire
		return false;
	}
	if (/^post:/.test(url)) { // requête de type POST
		var u = url.replace(/^post:/, '');
		var form = $('<form method="POST" action="' + getContextPath() + u + '"/>');
		form.appendTo('body');
		form.submit();
	}
	else if (/^goto:/.test(url)) { // requête de type GOTO
		var u = url.replace(/^goto:/, '');
		this.location.href=getContextPath() + u;
	}
}

//===================================================

var DisplayTable =  {

	buildPagination: function(page, pageSize, totalCount, buildGotoPageStatement) {

		var html = '';

		if (totalCount > pageSize) {
			html += '<table class="pageheader" style="margin-top: 0px;"><tr>\n';
			html += '<td class="pagebanner">Trouvé ' + totalCount + ' éléments. Affichage de ' + ((page - 1) * pageSize + 1) + ' à ' + (page * pageSize) + '.</td>';
			html += '<td class="pagelinks">&nbsp;\n';

			var pageCount = Math.ceil(totalCount / pageSize);
			var firstShownPage = Math.max(1, page - 5);
			var lastShownPage = Math.min(pageCount, page + 5);

			// previous link
			if (page > 1) {
				html += '<a href="#" onclick="' + buildGotoPageStatement(1) + '; return false;">«&nbsp;premier</a>\n';
				html += '<a href="#" onclick="' + buildGotoPageStatement(page - 1) + '; return false;">‹&nbsp;précédent</a>\n';
			}

			// direct page links
			for (var i = firstShownPage; i <= lastShownPage; ++i) {
				if (i == page) {
					html += '<font size="+1"><strong>' + i + '</strong></font>&nbsp;\n';
				}
				else {
					html += '<a href="#" onclick="' + buildGotoPageStatement(i) + '; return false;">' + i + '</a>&nbsp;\n';
				}
			}

			// next link
			if (page < pageCount) {
				html += '<a href="#" onclick="' + buildGotoPageStatement(page + 1) + '; return false;">suivant&nbsp;›</a>\n';
				html += '<a href="#" onclick="' + buildGotoPageStatement(pageCount) + '; return false;">dernier&nbsp;»</a>\n';
			}

			html += '</td></tr></table>';
		}
		else if (totalCount == 0) {
			html += '<table class="pageheader" style="margin-top: 0px;"><tr>\n';
			html += '<td class="pagebanner">Aucun élément trouvé.</td>';
			html += '</td></tr></table>';
		}

		return html;
	}
}