<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<!-- Debut Immeubles -->

<div id="immeublesDiv" style="position:relative"><img src="<c:url value="/images/loading.gif"/>"/></div>

<script>

	// chargement Ajax des immeubles
	$(function() {
		Immeubles.refreshTable(1);
	});

    var Immeubles = {

        refreshTable: function (page) {
            // get the data
            $.get('<c:url value="/rf/immeuble/list.do?ctb=${command.tiersGeneral.numero}"/>' + '&page=' + page + '&' + new Date().getTime(), function(immeublesPage) {
                var html = '<fieldset>\n';
                html += '<legend><span><fmt:message key="label.liste.immeubles" /></span></legend>\n';
                html += Immeubles.buildPageHeader(immeublesPage.page, 10, immeublesPage.totalCount);
                html += Immeubles.buildPageTable(immeublesPage.immeubles) + '\n';
                html += '</fieldset>\n';
                $('#immeublesDiv').html(html);
            }, 'json')
            .error(Ajax.popupErrorHandler);
            return false;
        },

        buildPageHeader: function (page, pageSize, totalCount) {
            return DisplayTable.buildPagination(page, pageSize, totalCount, function(i) {
                return 'Immeubles.refreshTable(' + i + ')';
            });
        },

        buildPageTable: function (immeubles) {

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
                html += '<td>' + StringUtils.escapeHTML(immeuble.noCommune) + ' ' + StringUtils.escapeHTML(immeuble.nomCommune) + '</td>';
                html += '<td>' + StringUtils.escapeHTML(immeuble.numero) + '</td>';
                html += '<td>' + StringUtils.escapeHTML(immeuble.typeImmeuble) + '</td>';
                html += '<td>' + StringUtils.escapeHTML(immeuble.nature) + '</td>';
                html += '<td>' + StringUtils.escapeHTML(immeuble.genrePropriete) + '</td>';
                html += '<td>' + StringUtils.escapeHTML(immeuble.partPropriete) + '</td>';
                html += '<td class="number">' + StringUtils.escapeHTML(immeuble.estimationFiscale) + '</td>';
                html += '<td>' + StringUtils.escapeHTML(immeuble.referenceEstimationFiscale) + '</td>';
                html += '<td>' + StringUtils.escapeHTML(immeuble.dateDernierMutation) + '</td>';
                html += '<td>' + StringUtils.escapeHTML(immeuble.derniereMutation) + '</td>';
                html += '<td>' + StringUtils.escapeHTML(immeuble.dateDebut) + '</td>';
                html += '<td>' + StringUtils.escapeHTML(immeuble.dateFin) + '</td>';
                html += '<td style="width:38px" class="action">';
                html += '<a href="' + immeuble.lienRF + '" class="extlink" title="Lien vers le registre foncier" style="margin-right: 0.5em;" target="_blank">&nbsp;</a>';
                html += '<a href="#" class="consult" title="Consultation des logs" onclick="return Dialog.open_consulter_log(\'Immeuble\', ' + immeuble.id + ');">&nbsp;</a>';
                html += '</td>';
                html += '</tr>\n';
            }

            html += '</tbody></table>\n';

            return html;
        }
    }

</script>

<!-- Fin Immeubles -->
