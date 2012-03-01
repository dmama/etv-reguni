<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<!-- Debut Dossiers Apparentes -->
<c:if test="${command.allowedOnglet.DOS || command.allowedOnglet.DBT}">
	<table border="0">
		<tr><td>
			<c:if test="${empty param['message'] && empty param['retour']}">
					<unireg:raccourciModifier link="../dossiers-apparentes/edit.do?id=${command.tiers.numero}" tooltip="Modifier les dossiers apparentés" display="label.bouton.modifier"/>
			</c:if>
		</td></tr>
	</table>
</c:if>

<div id="rapportsDiv" style="position:relative"><img src="<c:url value="/images/loading.gif"/>"/></div>
<div id="filiationsDiv" style="position:relative"></div>
<div id="debiteursDiv" style="position:relative"></div>

<script>
	// chargement Ajax des rapports-entre-tiers
	$(function() {
		loadRapports(1);
		loadFiliations();
		loadDebiteurs();
	});

	function loadRapports(page) {
		$('#rapportsSpinner').show();
		var showHisto = $('#isRapportHisto').attr('checked') ? 'true' : 'false';
		var type = $('#typeRapportId').val();

		// get the data
		$.get('<c:url value="/rapport/rapports.do?tiers=${command.tiersGeneral.numero}"/>' + '&page=' + page + '&showHisto=' + showHisto + '&type=' + type + '&' + new Date().getTime(),
			function(rapportsPage) {
				var html = '<fieldset>\n';
				html += '<legend><span><fmt:message key="label.dossiers.apparentes" /></span></legend>\n';
				html += '<div id="rapportsSpinner" style="position:absolute;right:1.5em;width:24px;display:none"><img src="<c:url value="/images/loading.gif"/>"/></div>';
				html += buildRapportsOptions(rapportsPage.page, rapportsPage.showHisto, rapportsPage.typeRapport, rapportsPage.typesRapportEntreTiers);
				if (rapportsPage.totalCount > 0) {
					html += buildRapportsPagination(rapportsPage.page, 10, rapportsPage.totalCount);
					html += buildRapportsTable(rapportsPage.rapports) + '\n';
				}
				else {
					html += escape("<fmt:message key="label.dossiers.apparentes.vide"/>");
				}
				html += '</fieldset>\n'
				$('#rapportsDiv').html(html);
			});
		return false;
	}

	function buildRapportsOptions(page, showHisto, typeSelectionne, typesRapportEntreTiers) {
		var html = '<table><tr>\n';
		html += '<td width="25%"><input class="noprint" type="checkbox" id="isRapportHisto"' + (showHisto ? ' checked="true"' : '') + ' onclick="return loadRapports(' + page + ');"> ';
		html += '<label class="noprint" for="isRapportHisto">Historique</label></td>\n';
		html += '<td width="75%">&nbsp;</td>\n';
		html += '</tr><tr>\n';
		html += '<td width="25%">Type de rapport entre tiers&nbsp;:</td>\n';
		html += '<td width="75%"><form name="form" id="form"><select name="typeRapport" id="typeRapportId" onchange="return loadRapports(' + page + ');">\n';
		html += '<option value="">Tous</option>\n';

		for (i in typesRapportEntreTiers) {
			var type = typesRapportEntreTiers[i];
			html += '<option value="' + i + '"' + (i == typeSelectionne ? ' selected' : '') + '>' + escape(type) + '</option>\n';
		}

		html += '</select></form></td>\n';
		html += '</tr></tbody></table>\n';
		return html;
	}

	function buildRapportsPagination(page, pageSize, totalCount) {
		return DisplayTable.buildPagination(page, pageSize, totalCount, function(i) {
			return 'loadRapports(' + i + ')';
		});
	}

	function buildRapportsTable(rapports) {

		var hasExtensionExecutionForcee = false;
		var hasAutoriteTutelaire = false;
		for (var i in rapports) {
			var rapport = rapports[i];
			hasExtensionExecutionForcee = hasExtensionExecutionForcee || rapport.extensionExecutionForcee;
			hasAutoriteTutelaire = hasAutoriteTutelaire || rapport.autoriteTutelaireId;
		}

		var html = '<table id="rapport" class="display"><thead><tr>\n';
		html += '<th>Rapport avec le tiers</th>';
		html += '<th>Date début</th>';
		html += '<th>Date fin</th>';
		html += '<th>N° de tiers</th>';
		html += '<th>Nom / Raison sociale</th>';
		if (hasAutoriteTutelaire) {
			html += '<th>Autorité tutelaire</th>';
		}
		if (hasExtensionExecutionForcee) {
			html += '<th>Extension à l\'exécution forcée</th>';
		}
		html += '<th></th>';
		html += '</tr></thead>\n';

		html += '<tbody>\n';

		for (var i in rapports) {
			var rapport = rapports[i];
			html += '<tr class="' + (i % 2 == 0 ? 'odd' : 'even') +'">';

			html += '<td>' + escape(rapport.type);
			if (rapport.toolTipMessage) {
				var filId = 'ret-' + i;
				html += ' <a href="#tooltip" class="staticTip" id="' + filId +'">?</a><div id="' + filId + '-tooltip" style="display:none;">' + rapport.toolTipMessage + '</div></td>';
			}
			html += '</td>';

			html += '<td>' + escape(rapport.dateDebut) + '</td>';
			html += '<td>' + escape(rapport.dateFin) + '</td>';
			html += '<td>' + Tiers.linkTo(rapport.numeroAutreTiers) + '</td>';

			html += '<td>';
			if (rapport.nomCourrier.nomCourrier1) {
				html += escape(rapport.nomCourrier.nomCourrier1);
			}
			if (rapport.nomCourrier.nomCourrier2) {
				html += '<br/>' + escape(rapport.nomCourrier.nomCourrier2);
			}
			html += '</td>';

			if (hasAutoriteTutelaire) {
				html += '<td>';
				if (rapport.nomAutoriteTutelaire) {
					html += escape(rapport.nomAutoriteTutelaire);
				}
				html += '</td>';
			}
			if (hasExtensionExecutionForcee) {
				html += '<td>';
				if (rapport.extensionExecutionForcee != null) {
					html += '<input type="checkbox"' + (rapport.extensionExecutionForcee ? ' checked="true"' : '') + ' disabled="true"/>';
				}
				html += '</td>';
			}

			html += '<td><a href="#" class="consult" title="Consultation des logs" onclick="return Dialog.open_consulter_log(\'RapportEntreTiers\', ' + rapport.id + ');">&nbsp;</a></td>';
			html += '</tr>\n';
		}

		return html;
	}

	function loadFiliations() {
		// get the data
		$.get('<c:url value="/rapport/filiations.do?tiers=${command.tiersGeneral.numero}"/>' + '&' + new Date().getTime(), function(filiations) {
			var html = '';
			if (typeof filiations === 'string') {
				// on a reçu une erreur
				html += '<fieldset>\n';
				html += '<legend><span><fmt:message key="label.filiations" /></span></legend>\n';
				html += '<div class="flash-warning">' + escape("<fmt:message key="label.affichage.filiations.impossible"/>") + '<br/><i>' + escape(filiations) + '</i></div>\n';
				html += '</fieldset>\n'
			}
			else {
				// on a bien reçu les filiations
				if (filiations.length) {
					html += '<fieldset>\n';
					html += '<legend><span><fmt:message key="label.filiations" /></span></legend>\n';
					html += buildTableFiliations(filiations) + '\n';
					html += '</fieldset>\n'
				}
			}
			$('#filiationsDiv').html(html);
		});
		return false;
	}

	function buildTableFiliations(filiations) {

		var html = '<table id="filiation" class="display"><thead><tr>\n';
		html += '<th>Rapport avec le tiers</th>';
		html += '<th>Date début</th>';
		html += '<th>Date fin</th>';
		html += '<th>N° de tiers</th>';
		html += '<th>Nom / Raison sociale</th>';
		html += '</tr></thead>\n';

		html += '<tbody>\n';

		for (var i in filiations) {
			var filiation = filiations[i];
			html += '<tr class="' + (i % 2 == 0 ? 'odd' : 'even') +'">';
			html += '<td>' + (filiation.type == 'ENFANT' ? 'Enfant' : 'Parent');
			if (filiation.toolTipMessage) {
				var filId = 'fil-' + i;
				html += ' <a href="#tooltip" class="staticTip" id="' + filId +'">?</a><div id="' + filId + '-tooltip" style="display:none;">' + filiation.toolTipMessage + '</div></td>';
			}
			html += '<td>' + escape(filiation.dateDebut) + '</td>';
			html += '<td>' + escape(filiation.dateFin) + '</td>';
			if (filiation.numeroAutreTiers) {
				html += '<td>' + Tiers.linkTo(filiation.numeroAutreTiers) + '</td>';
			}
			else {
				html += '<td><div class="flash-warning">' + escape(filiation.messageAutreTiersAbsent) + '</div></td>';
			}
			html += '<td>' + escape(filiation.nomCourrier.nomCourrier1);
			if (filiation.nomCourrier.nomCourrier2) {
				html += '<br/>' + escape(filiation.nomCourrier.nomCourrier2);
			}
			html += '</td>';
			html += '</tr>\n';
		}

		return html;
	}

	function loadDebiteurs() {
		// get the data
		$.get('<c:url value="/rapport/debiteurs.do?tiers=${command.tiersGeneral.numero}"/>' + '&' + new Date().getTime(), function(debiteurs) {
			var html = '';
			if (debiteurs.length) {
				html += '<fieldset>\n';
				html += '<legend><span><fmt:message key="label.debiteur.is" /></span></legend>\n';
				html += buildTableDebiteurs(debiteurs) + '\n';
				html += '</fieldset>\n'
			}
			$('#debiteursDiv').html(html);
			Tooltips.activate_static_tooltips();
		});
		return false;
	}

	function buildTableDebiteurs(debiteurs) {

		var html = '<table id="debiteur" class="display"><thead><tr>\n';
		html += '<th>N° de débiteur</th>';
		html += '<th>Nom / Raison sociale</th>';
		html += '<th>Catégorie IS</th>';
		html += '<th>Contact</th>';
		html += '<th></th>';
		html += '</tr></thead>\n';

		html += '<tbody>\n';

		for (var i in debiteurs) {
			var debiteur = debiteurs[i];
			html += '<tr class="' + (i % 2 == 0 ? 'odd' : 'even') +'">';
			html += '<td>' + Tiers.linkTo(debiteur.numero) + '</td>';
			html += '<td>' + escape(debiteur.nomCourrier1);
			if (debiteur.nomCourrier2) {
				html += '<br/>' + escape(debiteur.nomCourrier2);
			}
			if (debiteur.complementNom) {
				html += '<br/>' + escape(debiteur.complementNom);
			}
			html += '<td>' + escape(debiteur.nomCategorie) + '</td>';
			html += '<td>' + escape(debiteur.personneContact) + '</td>';
			html += '<td><a href="#" class="consult" title="Consultation des logs" onclick="return Dialog.open_consulter_log(\'RapportEntreTiers\', ' + debiteur.id + ');">&nbsp;</a></td>';
			html += '</tr>\n';
		}

		return html;
	}

	function escape(value) {
		var html = '';
		if (value) {
			html += StringUtils.escapeHTML(value);
		}
		return html;
	}

</script>

<!-- Fin Dossiers Apparentes -->
