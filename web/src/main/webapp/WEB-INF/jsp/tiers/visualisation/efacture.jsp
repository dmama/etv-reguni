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
                if (destinataire !== null) {
                    <authz:authorize ifAnyGranted="ROLE_GEST_EFACTURE">
                        html += '<table border="0"><tr><td>';
                        html += '<unireg:raccourciModifier link="../efacture/edit.do?ctb=' + destinataire.ctbId + '" tooltip="Interagir avec les états e-Facture" display="label.bouton.modifier"/>';
                        html += '</td></tr></table>';
                    </authz:authorize>
                    const url = document.location.href;
                    const printview = url.indexOf("printview=true") > -1;
                    html += '<fieldset>\n';
                    // d'abord le destinataire
                    if (printview) {
                        html += '<legend><span><fmt:message key="label.efacture.historique.destinataire.mode.print" /></span></legend>\n';
                    }
                    else{
                        html += '<legend><span><fmt:message key="label.efacture.historique.destinataire" /></span></legend>\n';
                    }

                    html += eFacture.buildHistoriqueDestinataire(destinataire.etats);
                    html += '</fieldset>\n';

                    // puis ses demandes individuelles
                    html += '<fieldset>\n';
                    if (printview) {
                        html += '<legend><span><fmt:message key="label.efacture.historique.demandes.mode.print" /></span></legend>\n';
                    }
                    else{
                        html += '<legend><span><fmt:message key="label.efacture.historique.demandes" /></span></legend>\n';
                    }

                    html += eFacture.buildHistoriqueDemandes(destinataire.ctbId, destinataire.demandes, printview);
                    html += '</fieldset>\n';
                }
                else {
                    html = '<span style="font-style: italic;">Aucune information e-facture disponible pour ce contribuable.</span>';
                }
                $('#efactureDiv').html(html);
            }, 'json')
            .error(eFacture.errorHandlerHisto);
            return false;
        },

        /**
         * Error handler qui affiche le message d'erreur ajax dans l'onglet e-Facture
         */
        errorHandlerHisto: function(xhr, ajaxOptions, thrownError) {
            var html = '<span class="error">Impossible d\'obtenir l\'historique des états e-Facture du contribuable :<br/><br/>' +
                       '&nbsp;&nbsp;&nbsp;&nbsp;<i>' + StringUtils.escapeHTML(thrownError) + ' (' +  StringUtils.escapeHTML(xhr.status) +') : ' +
                       StringUtils.escapeHTML(xhr.responseText) + '</i></span>';
            $('#efactureDiv').html(html);
        },

        buildHistoriqueDestinataire: function(etats) {

            var html = '<table id="efacture-destinataire" class="display"><thead><tr>\n';
            html += '<th style="width:20%"><fmt:message key="label.efacture.date.obtention"/></th>';
            html += '<th style="width:20%"><fmt:message key="label.efacture.etat"/></th>';
            html += '<th style="width:30%"><fmt:message key="label.efacture.motifTransition"/></th>';
            html += '<th style="width:25%"><fmt:message key="label.efacture.email"/></th>';
            html += '<th/>';
            html += '</tr></thead>\n';
            html += '<tbody>\n';

            for (var e in etats) {
                var etat = etats[e];
                html += '<tr class="' + (e % 2 == 0 ? 'odd' : 'even') + '">';
                html += '<td>' + DateUtils.toNormalString(new Date(etat.dateObtention)) + '</td>';
                html += '<td>' + StringUtils.escapeHTML(etat.descriptionEtat) + '</td>';
                html += '<td>' + StringUtils.escapeHTML(etat.motifObtention) + '</td>';
                html += '<td>' + StringUtils.escapeHTML(etat.email) + '</td>';
                html += '<td>&nbsp;</td>';
                html += '</tr>';
            }

            html += '</tbody></table>\n';
            return html;
        },

        buildHistoriqueDemandes: function(noCtb, demandes, printview) {

            var html = '<table id="efacture-demandes" class="display"><thead><tr>\n';
            if (!printview) {
	            html += '<th style="width:2em;">&nbsp;</th>';
            }
            html += '<th width="10%"><fmt:message key="label.efacture.date.demande"/></th>';
	        html += '<th><fmt:message key="label.efacture.type.demande"/></th>';
	        html += '<th><fmt:message key="label.efacture.avs"/></th>';
	        html += '<th><fmt:message key="label.efacture.email"/></th>';
	        html += '<th><fmt:message key="label.efacture.etat.courant"/></th>';
            html += '<th width="10%"><fmt:message key="label.efacture.date.obtention.etat.courant"/></th>';
            html += '<th><fmt:message key="label.efacture.motifTransition"/></th>';
            html += '</tr></thead>\n';
            html += '<tbody>\n';

            for (var d in demandes) {
                var demande = demandes[d];
                html += '<tr class="' + (d % 2 == 0 ? 'odd' : 'even') + '">';
                if (!printview) {
	                html += '<td style="height:20px;"><img style="vertical-align: top;" id="toggle_ef_' + demande.idDemande + '" src="<c:url value="/images/plus.gif"/>" onclick="eFacture.toggleShowDetailDemande(' + demande.idDemande + ');"/>&nbsp;</td>';
                }
                html += '<td>' + RegDate.format(demande.dateDemande) + '</td>';
	            html += '<td title="ID : ' + StringUtils.escapeHTML(demande.idDemande) + '">' + StringUtils.escapeHTML(demande.descriptionTypeDemande) + '</td>';
	            html += '<td>' + StringUtils.escapeHTML(demande.avs) + '</td>';
	            html += '<td>' + StringUtils.escapeHTML(demande.email) + '</td>';

                var etatCourant = demande.etatCourant;
                html += '<td>' + StringUtils.escapeHTML(etatCourant.descriptionEtat) + '</td>';
                html += '<td>' + DateUtils.toNormalString(new Date(etatCourant.dateObtention)) + '</td>';
                html += '<td>' + StringUtils.escapeHTML(etatCourant.motifObtention) + '</td>';
                html += '</tr>\n';

                html += '<tr ' + (printview ? '' : 'style="display: none;" ') + 'class="' + (d % 2 == 0 ? 'odd' : 'even') + '" id="detail_ef_' + demande.idDemande + '">';
                if (!printview) {
	                html += '<td>&nbsp;</td>';
                }
                html += '<td colspan="7" style="text-align: center;">';
                html += eFacture.buildHistoriqueEtatsDemande(noCtb, demande.idDemande, demande.etats);
                html += '</td>';
                html += '</tr>\n';
            }

            html += '</tbody></table>\n';
            return html;
        },

        buildHistoriqueEtatsDemande: function(noCtb, idDemande, etats) {
            var html = '<table id="efacture-demande_' + idDemande + '" class="display" style="margin: 10px auto 10px auto; width: 90%;"><thead><tr>\n';
            html += '<th width="15%"><fmt:message key="label.efacture.date.obtention"/></th>';
            html += '<th><fmt:message key="label.efacture.etat"/></th>';
            html += '<th><fmt:message key="label.efacture.motifTransition"/></th>';
            html += '<th/>';
            html += '</tr></thead>\n';
            html += '<tbody>\n';

            for (var e in etats) {
                var etat = etats[e];
                html += '<tr class="' + (e % 2 == 0 ? 'odd' : 'even') + '">';
                html += '<td>' + DateUtils.toNormalString(new Date(etat.dateObtention)) + '</td>';
                html += '<td>' + StringUtils.escapeHTML(etat.descriptionEtat) + '</td>';
                html += '<td>' + StringUtils.escapeHTML(etat.motifObtention) + '</td>';
                if (etat.urlVisualisationExterneDocument != null) {
                	html += '<td><a href="#" title="Visualisation du courrier émis" class="pdf" onclick="VisuExterneDoc.openWindow(\x27' + etat.urlVisualisationExterneDocument + '\x27);">&nbsp;</a></td>';
                }
                else if (etat.documentArchiveKey != null) {
                    html += '<td>';
                    var idIcon = 'print-doc-' + idDemande + '-' + e;
                    var url = '<c:url value="/copie-conforme.do"/>?noCtb=' + noCtb + '&typeDoc=' + StringUtils.escapeHTML(etat.documentArchiveKey.typeDocument) + '&key=' + StringUtils.escapeHTML(etat.documentArchiveKey.key) + '&url_memorize=false';
                    html += '<a class="pdf" id="' + idIcon + '" href="' + url + '" onclick=\'Link.tempSwap(this, "#disabled-' + idIcon + '");\'>&nbsp;</a>\n';
                    html += '<span class="pdf-grayed" id="disabled-' + idIcon + '" style="display: none;" >&nbsp;</span>\n';
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
