<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<c:set var="tiersId" value="${param.tiersId}"/>

	<div id="remarques">

		<authz:authorize ifAnyGranted="ROLE_COOR_FIN, ROLE_MODIF_AC, ROLE_MODIF_VD_ORD, ROLE_MODIF_VD_SOURC, ROLE_MODIF_HC_HS, ROLE_MODIF_HAB_DEBPUR, ROLE_MODIF_NONHAB_DEBPUR, ROLE_MODIF_PM, ROLE_MODIF_CA, ROLE_MODIF_NONHAB_INACTIF">
			<a id="addRemarque" class="add noprint" href="#">Ajouter une remarque</a>
			<div id="newRemarque" class="new_remarque" style="display:none;">
				<textarea cols="80" rows="3"></textarea><br>
				<input type="button" value="Ajouter"/>&nbsp;ou&nbsp;<a href="#">annuler</a>
			</div>
		</authz:authorize>

		<div id="remarquesContentDiv"><img src="<c:url value="/images/loading.gif"/>"/>Chargement des remarques...</div>

	</div>

<script>

	var Remarque = {

		refreshRemarques: function() {
			$.get('<c:url value="/remarque/list.do?tiersId="/>${tiersId}&' + new Date().getTime(), function(list) {

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
						table += '<tr class="' + (i % 2 == 0 ? 'even' : 'odd') + (rem.annule ? ' strike' : '') + '">';
						table += '<td class="entete">le ' + Remarque.escapeHTML(rem.date) + ' par ' + Remarque.escapeHTML(rem.user) + '</td>';
						if (rem.nbLines < rem.thresholdNbLines) {
							table += '<td class="texte">' + rem.htmlText + '</td>';
						}
						else {
							table += '<td class="texte">';
							table += '<div id="rq-short-' + i + '">' + rem.shortHtmlText + Remarque.buildToggle(i) + '</div>';
							table += '<div id="rq-long-' + i + '" style="display:none;">' + rem.htmlText + '</div>';
							table += '</td>';
						}
						table += '<td style="width:2em;"><a href="#" class="consult" title="Consultation des logs" onclick="return Dialog.open_consulter_log(\'Remarque\', ' + rem.id + ');">&nbsp;</a></td>';
						table += '</tr>';
					}
					table += '</tbody></table>';
					$('#remarquesContentDiv').html(table);
				}

				$('#addRemarque').show();
				var newRemarque = $('#newRemarque');
				newRemarque.hide();
				newRemarque.find('textarea').val('');
			}, 'json')
			.error(Ajax.popupErrorHandler);
		},

		escapeHTML: function(text) {
			return StringUtils.escapeHTML(text);
		},

		buildToggle: function(index) {
			return '<a title="Visualiser la fin de la remarque" class="ellipsis" href="#" onclick="Remarque.toggleLongVersion(' + index + ')">&nbsp;</a>';
		},

		toggleLongVersion: function(index) {
			$('td #rq-short-' + index).hide();
			$('td #rq-long-' + index).show();
		}
	};

	$('#addRemarque').click(function() {
		$('#addRemarque').hide();
		$('#newRemarque').show();
		return false;
	});

	$('#newRemarque').find('input').click(function() {
		var text = $('#newRemarque').find('textarea').val();
		$.post('<c:url value="/remarque/add.do"/>', {'tiersId': ${tiersId}, 'text': text}, function() {
			// on success, refresh all
			Remarque.refreshRemarques();
		});
		return false;
	});

	$('#newRemarque').find('a').click(function() {
		$('#addRemarque').show();
		$('#newRemarque').hide();
		return false;
	});

	Remarque.refreshRemarques();

</script>
