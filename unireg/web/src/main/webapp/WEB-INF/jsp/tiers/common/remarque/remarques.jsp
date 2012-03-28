<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<c:set var="tiersId" value="${param.tiersId}"/>

	<div id="remarques">

		<authz:authorize ifAnyGranted="ROLE_COOR_FIN, ROLE_MODIF_AC, ROLE_MODIF_VD_ORD, ROLE_MODIF_VD_SOURC, ROLE_MODIF_HC_HS, ROLE_MODIF_HAB_DEBPUR, ROLE_MODIF_NONHAB_DEBPUR, ROLE_MODIF_PM, ROLE_MODIF_CA, ROLE_MODIF_NONHAB_INACTIF">
			<a id="addRemarque" class="add noprint" classname="add noprint" href="#">&nbsp;Ajouter une remarque</a>
			<div id="newRemarque" class="new_remarque" style="display:none;">
				<textarea cols="80" rows="3"></textarea><br>
				<input type="button" value="Ajouter"/>&nbsp;ou&nbsp;<a href="#">annuler</a>
			</div>
		</authz:authorize>

		<div id="remarquesContentDiv"><img src="<c:url value="/images/loading.gif"/>"/>Chargement des remarques...</div>

	</div>

<script>

	function refreshRemarques() {
		$.get('<c:url value="/remarque/list.do?tiersId="/>' + ${tiersId} + '&' + new Date().getTime(), function(list) {

			var count = list.length;
			if (count == 0) {
				$('#remarqueTabAnchor').text('Remarques');
				$('#remarquesContentDiv').text("(aucune remarque n'a été saisie pour l'instant)");
			}
			else {
				$('#remarqueTabAnchor').text('Remarques (' + count + ')');

				// rebuild the table from scratch
				var table = '<table class="remarques" border="0" cellspacing="0"><tbody>';
				for (var i = 0; i < count; ++i) {
					var rem = list[i];
					table += '<tr class="' + (i % 2 == 0 ? 'even' : 'odd') + '">';
					table += '<td class="entete">le ' + escapeHTML(rem.date) + ' par ' + escapeHTML(rem.user) + '</td><td class="texte">' + escapeHTML(rem.text) + '</td>';
					table += '</tr>';
				}
				table += '</tbody></table>';
				$('#remarquesContentDiv').html(table);
			}

			$('#addRemarque').show();
			$('#newRemarque').hide();
			$('#newRemarque textarea').val('');
		}, 'json')
		.error(App.ajaxErrorHandler);
	}

	function escapeHTML(text) {
		return StringUtils.escapeHTML(text);
	}

	$('#addRemarque').click(function() {
		$('#addRemarque').hide();
		$('#newRemarque').show();
		return false;
	});

	$('#newRemarque input').click(function() {
		var text = $('#newRemarque textarea').val();
		$.post('<c:url value="/remarque/add.do"/>', {'tiersId': ${tiersId}, 'text': text}, function() {
			// on success, refresh all
			refreshRemarques();
		});
		return false;
	});

	$('#newRemarque a').click(function() {
		$('#addRemarque').show();
		$('#newRemarque').hide();
		return false;
	});

	refreshRemarques();

</script>
