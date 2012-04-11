<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<!-- Debut Immeubles -->

<div id="immeublesDiv" style="position:relative"><img src="<c:url value="/images/loading.gif"/>"/></div>

<script>
	// chargement Ajax des immeubles
	$(function() {
		refreshTable(1);
	});

	function refreshTable(page) {
		// get the data
		$.get('<c:url value="/rf/immeuble/list.do?ctb=${command.tiersGeneral.numero}"/>' + '&page=' + page + '&' + new Date().getTime(), function(immeublesPage) {
			var html = '<fieldset>\n';
			html += '<legend><span><fmt:message key="label.liste.immeubles" /></span></legend>\n';
			html += buildPageHeader(immeublesPage.page, 10, immeublesPage.totalCount);
			html += buildPageTable(immeublesPage.immeubles) + '\n';
			html += '</fieldset>\n'
			$('#immeublesDiv').html(html);
		}, 'json')
		.error(Ajax.popupErrorHandler);
		return false;
	}

	function buildPageHeader(page, pageSize, totalCount) {
		return DisplayTable.buildPagination(page, pageSize, totalCount, function(i) {
			return 'refreshTable(' + i + ')';
		});
	}

	function buildPageTable(immeubles) {

		var html = '<table id="immeuble" class="display"><thead><tr>\n';
		html += '<th>Commune</th>';
		html += '<th>N° d\'immeuble</th>';
		html += '<th>Type</th>';
		html += '<th>Nature</th>';
		html += '<th>Genre de propriété</th>';
		html += '<th>Part</th>';
		html += '<th>Estimation fiscale (CHF)</th>';
		html += '<th>Référence EF</th>';
		html += '<th>Date dernière mutation</th>';
		html += '<th>Type dernière mutation</th>';
		html += '<th>Date début</th>';
		html += '<th>Date fin</th>';
		html += '<th></th>';
		html += '</tr></thead>\n';

		html += '<tbody>\n';

		for (var i in immeubles) {
			var immeuble = immeubles[i];
			html += '<tr class="' + (i % 2 == 0 ? 'odd' : 'even') +'">';
			html += '<td>' + escape(immeuble.noCommune) + ' ' + escape(immeuble.nomCommune) + '</td>';
			html += '<td>' + escape(immeuble.numero) + '</td>';
			html += '<td>' + escape(immeuble.typeImmeuble) + '</td>';
			html += '<td>' + escape(immeuble.nature) + '</td>';
			html += '<td>' + escape(immeuble.genrePropriete) + '</td>';
			html += '<td>' + escape(immeuble.partPropriete) + '</td>';
			html += '<td class="number">' + escape(immeuble.estimationFiscale) + '</td>';
			html += '<td>' + escape(immeuble.referenceEstimationFiscale) + '</td>';
			html += '<td>' + escape(immeuble.dateDernierMutation) + '</td>';
			html += '<td>' + escape(immeuble.derniereMutation) + '</td>';
			html += '<td>' + escape(immeuble.dateDebut) + '</td>';
			html += '<td>' + escape(immeuble.dateFin) + '</td>';
			html += '<td style="width:38px" class="action">';
			html += '<a href="' + immeuble.lienRF + '" class="extlink" title="Lien vers le registre foncier" style="margin-right: 0.5em;" target="_blank">&nbsp;</a>';
			html += '<a href="#" class="consult" title="Consultation des logs" onclick="return Dialog.open_consulter_log(\'Immeuble\', ' + immeuble.id + ');">&nbsp;</a>';
			html += '</td>';
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

<!-- Fin Immeubles -->
