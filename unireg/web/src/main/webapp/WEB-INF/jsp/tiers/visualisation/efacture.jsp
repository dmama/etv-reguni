<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<!-- Debut e-Facture -->

<div id="efactureDiv" style="position:relative"><img src="<c:url value="/images/loading.gif"/>"/></div>

<script>
	// chargement Ajax des données e-facture
	$(function() {
		eFacture.refreshTable();
	});

    var eFacture = {

        refreshTable: function() {
            // get the data
            $.get('<c:url value="/efacture/histo.do?ctb=${command.tiersGeneral.numero}"/>' + '&' + new Date().getTime(), function(destinataire) {
                var html = '';
                if (destinataire != null) {
                    <authz:authorize ifAnyGranted="ROLE_GEST_EFACTURE">
                        html += '<table border="0"><tr><td>';
                        html += '<unireg:raccourciModifier link="../efacture/edit.do?ctb=' + destinataire.ctbId + '" tooltip="Interagir avec les états e-Facture" display="label.bouton.modifier"/>';
                        html += '</td></tr></table>';
                    </authz:authorize>

                    html += '<fieldset>\n';
                    // d'abord le destinataire
                    html += '<legend><span><fmt:message key="label.efacture.historique.destinataire" /></span></legend>\n';
                    html += eFacture.buildHistoriqueDestinataire(destinataire.etats);
                    html += '</fieldset>\n';

                    // puis ses demandes individuelles
                    html += '<fieldset>\n';
                    html += '<legend><span><fmt:message key="label.efacture.historique.demandes" /></span></legend>\n';
                    html += eFacture.buildHistoriqueDemandes(destinataire.ctbId, destinataire.demandes);
                    html += '</fieldset>\n';
                }
                else {
                    html = '<span style="font-style: italic;">Aucune information e-facture disponible pour ce contribuable.</span>';
                }
                $('#efactureDiv').html(html);
            }, 'json')
            .error(Ajax.popupErrorHandler);
            return false;
        },

        buildHistoriqueDestinataire: function(etats) {

            var html = '<table id="efacture-destinataire" class="display"><thead><tr>\n';
            html += '<th width="10%"><fmt:message key="label.efacture.date.obtention"/></th>';
            html += '<th><fmt:message key="label.efacture.etat"/></th>';
            html += '<th><fmt:message key="label.efacture.motifTransition"/></th>';
            html += '<th/>';
            html += '</tr></thead>\n';
            html += '<tbody>\n';

            for (var e in etats) {
                var etat = etats[e];
                html += '<tr class="' + (e % 2 == 0 ? 'odd' : 'even') + '">';
                html += '<td>' + RegDate.format(etat.dateObtention) + '</td>';
                html += '<td>' + StringUtils.escapeHTML(etat.descriptionEtat) + '</td>';
                html += '<td>' + StringUtils.escapeHTML(etat.motifObtention) + '</td>';
                html += '<td>&nbsp;</td>';
                html += '</tr>';
            }

            html += '</tbody></table>\n';
            return html;
        },

        buildHistoriqueDemandes: function(noCtb, demandes) {

            var html = '<table id="efacture-demandes" class="display"><thead><tr>\n';
            html += '<th style="width:2em;">&nbsp;</th>';
            html += '<th width="10%"><fmt:message key="label.efacture.date.demande"/></th>';
            html += '<th><fmt:message key="label.efacture.etat.courant"/></th>';
            html += '<th width="10%"><fmt:message key="label.efacture.date.obtention.etat.courant"/></th>';
            html += '<th><fmt:message key="label.efacture.motifTransition"/></th>';
            html += '</tr></thead>\n';
            html += '<tbody>\n';

            for (var d in demandes) {
                var demande = demandes[d];
                html += '<tr class="' + (d % 2 == 0 ? 'odd' : 'even') + '">';
                html += '<td style="height:20px;"><img style="vertical-align: top;" id="toggle_ef_' + demande.idDemande + '" src="<c:url value="/images/plus.gif"/>" onclick="eFacture.toggleShowDetailDemande(' + demande.idDemande + ');"/>&nbsp;</td>';
                html += '<td>' + RegDate.format(demande.dateDemande) + '</td>';

                var etatCourant = demande.etatCourant;
                html += '<td>' + StringUtils.escapeHTML(etatCourant.descriptionEtat) + '</td>';
                html += '<td>' + RegDate.format(etatCourant.dateObtention) + '</td>';
                html += '<td>' + StringUtils.escapeHTML(etatCourant.motifObtention) + '</td>';
                html += '</tr>\n';

                html += '<tr style="display:none;" class="' + (d % 2 == 0 ? 'odd' : 'even') + '" id="detail_ef_' + demande.idDemande + '">';
                html += '<td>&nbsp;</td>';
                html += '<td colspan="4">';
                html += eFacture.buildHistoriqueEtatsDemande(noCtb, demande.idDemande, demande.etats);
                html += '</td>';
                html += '</tr>\n';
            }

            html += '</tbody></table>\n';
            return html;
        },

        buildHistoriqueEtatsDemande: function(noCtb, idDemande, etats) {
            var html = '<table id="efacture-demande_' + idDemande + '" class="display"><thead><tr>\n';
            html += '<th width="10%"><fmt:message key="label.efacture.date.obtention"/></th>';
            html += '<th><fmt:message key="label.efacture.etat"/></th>';
            html += '<th><fmt:message key="label.efacture.motifTransition"/></th>';
            html += '<th/>';
            html += '</tr></thead>\n';
            html += '<tbody>\n';

            for (var e in etats) {
                var etat = etats[e];
                html += '<tr class="' + (e % 2 == 0 ? 'odd' : 'even') + '">';
                html += '<td>' + RegDate.format(etat.dateObtention) + '</td>';
                html += '<td>' + StringUtils.escapeHTML(etat.descriptionEtat) + '</td>';
                html += '<td>' + StringUtils.escapeHTML(etat.motifObtention) + '</td>';
                if (etat.documentArchiveKey) {
                    html += '<td>';
                    var idIcon = 'print-doc-' + idDemande + '-' + e;
                    var url = '<c:url value="/copie-conforme.do"/>?noCtb=' + noCtb + '&key=' + StringUtils.escapeHTML(etat.documentArchiveKey);
                    html += '<a class="pdf" id="' + idIcon + '" href="' + url + '" onclick=\'Link.tempSwap(this, "#disabled-' + idIcon + '");\'>&nbsp;</a>\n';
                    html += '<span a class="pdf-grayed" id="disabled-' + idIcon + '" style="display: none;" >&nbsp;</span>\n';
                    html += '</td>';
                }
                else {
                    html += '<td>&nbsp;</td>';
                }
                html += '</tr>';
            }

            html += '</tbody></table>\n';
            return html;
        },

        toggleShowDetailDemande: function(idDemande) {
            var toggleImage = $('#toggle_ef_' + idDemande)[0];
            var detail = $('#detail_ef_' + idDemande)[0];
            if (detail.style.display == 'none') {
                detail.style.display = '';
                toggleImage.src = '<c:url value="/images/minus.gif"/>';
            }
            else {
                detail.style.display = 'none';
                toggleImage.src = '<c:url value="/images/plus.gif"/>';
            }
        }
    }

</script>

<!-- Fin e-Facture -->
