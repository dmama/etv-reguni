<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<fieldset>
	<legend><span><fmt:message key="label.recherche.simple"/></span></legend>
	<table><tr>
		<td style="vertical-align:middle"><input type="text" id="simple-search-input" width="100%" class="text ui-widget-content ui-corner-all" placeholder="Entrez vos critères de recherche ici" autofocus/></td>
		<td width="12px"></td>
		<td style="vertical-align:middle" width="2%"><a href="#" onclick="return toogle_search();" style="font-size:11px">recherche avancée</a></td>
	</tr></table>
</fieldset>

<div id="simple-search-results"><%-- cette DIV est mise-à-jour par Ajax --%></div>

<script type="text/javascript">

	var query = $('#simple-search-input');
	var last = "";

	// on installe la fonction qui sera appelée à chaque frappe de touche pour la recherche simple
	query.keyup(function() {
		var current = query.val();
		if (last != current) { // on ne rafraîchit que si le texte a vraiment changé
			last = current;

			clearTimeout($.data(this, "simple-search-timer"));
			// on retarde l'appel javascript de 200ms pour éviter de faire plusieurs requêtes lorsque l'utilisateur entre plusieurs caractères rapidemment
			var timer = setTimeout(function() {

				var queryString = '/search/quick.do?query=' + encodeURIComponent(current) + '&' + new Date().getTime();

				// on effectue la recherche par ajax
				$.get(getContextPath() + queryString, function(results) {
					$('#simple-search-results').html(build_html_simple_results(results));
				}, 'json')
				.error(function(xhr, ajaxOptions, thrownError){
					var message = '<span class="error">Oups ! La recherche a provoqué l\'erreur suivante :' +
							'&nbsp;<i>' + StringUtils.escapeHTML(thrownError) + ' (' +  StringUtils.escapeHTML(xhr.status) + ') : ' + StringUtils.escapeHTML(xhr.responseText) + '</i></span>';
					$('#simple-search-results').html(message);
				});

			}, 200); // 200 ms
			$.data(this, "simple-search-timer", timer);
		}
	});

	function build_html_simple_results(results) {
		var table = results.summary;

		if (results.entries.length > 0) {
			table += '<table border="0" cellspacing="0">';
			table += '<thead><tr class="header">';
			table += '<th>N° de tiers</th>' +
					'<th>Rôle</th>' +
					'<th>Nom / Raison sociale</th>' +
					'<th>Date naissance</th>' +
					'<th>NPA</th>' +
					'<th>Localité / Pays</th>' +
					'<th>For principal</th>' +
					'<th>Date ouv. for</th>' +
					'<th>Date ferm. for</th>' +
					'<th>Vers</th>';
			table += '</tr></thead>';
			table += '<tbody>';
			for(var i = 0; i < results.entries.length; ++i) {
				var e = results.entries[i];
				table += '<tr class="' + (i % 2 == 1 ? 'even' : 'odd') + (e.annule ? ' strike' : '') + '">';
				table += '<td><a href="visu.do?id=' + StringUtils.escapeHTML(e.numero) + '">' + Tiers.formatNumero(e.numero) + '</a></td>';
				table += '<td>' + StringUtils.escapeHTML(e.role1) + (e.role2 ? '<br>' + StringUtils.escapeHTML(e.role2) : '' ) + '</td>';
				table += '<td>' + StringUtils.escapeHTML(e.nom1) + (e.nom2 ? ' ' + StringUtils.escapeHTML(e.nom2) : '' ) + '</td>';
				table += '<td>' + StringUtils.escapeHTML(e.dateNaissance) + '</td>';
				table += '<td>' + StringUtils.escapeHTML(e.npa) + '</td>';
				table += '<td>' + StringUtils.escapeHTML(e.localitePays) + '</td>';
				table += '<td>' + StringUtils.escapeHTML(e.forPrincipal) + '</td>';
				table += '<td>' + RegDate.format(e.dateOuverture) + '</td>';
				table += '<td>' + RegDate.format(e.dateFermeture) + '</td>';
				table += '<td>';
				if (!e.annule) {
				<c:if test="${urlRetour == null}">
					table += '<select name="AppSelect" onchange="App.gotoExternalApp(this);">';
					table += '<option value="">---</option>';
					if (!e.debiteurInactif) {
						table += '<option value="<c:url value="/redirect/TAO_PP.do?id=' + e.numero + '"/>"><fmt:message key="label.TAOPP"/></option>';
						table += '<option value="<c:url value="/redirect/TAO_BA.do?id=' + e.numero + '"/>"><fmt:message key="label.TAOBA"/></option>';
						table += '<option value="<c:url value="/redirect/TAO_IS.do?id=' + e.numero + '"/>"><fmt:message key="label.TAOIS"/></option>';
					}
					table += '<option value="<c:url value="/redirect/SIPF.do?id=' + e.numero + '"/>"><fmt:message key="label.SIPF"/></option>';
					table += '<option value="launchcat.do?numero=' + StringUtils.escapeHTML(e.numero) + '"><fmt:message key="label.CAT"/></option>';
					table += '</select>';
				</c:if>
				<c:if test="${urlRetour != null}">
					table += '<a href="${urlRetour}' + StringUtils.escapeHTML(e.numero) + '" class="detail" title="<fmt:message key="label.retour.application.appelante" />">&nbsp;</a>';
				</c:if>
				}
				table += '</td>';
			}
			table += '</tbody></table>';
		}

		return table;
	}

</script>
