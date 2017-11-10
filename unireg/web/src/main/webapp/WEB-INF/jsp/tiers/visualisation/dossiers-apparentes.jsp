<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<unireg:setAuth var="autorisations" tiersId="${command.tiers.numero}"/>

<c:if test="${autorisations.debiteurs || autorisations.rapports || autorisations.rapportsEtablissements}">
    <table border="0">
        <tr><td>
            <c:if test="${empty param['message'] && empty param['retour']}">
                <unireg:raccourciModifier link="../rapport/list.do?id=${command.tiers.numero}" tooltip="Modifier les débiteurs IS" display="label.bouton.modifier"/>
            </c:if>
        </td></tr>
    </table>
</c:if>

<!-- Debut Dossiers Apparentes -->
<div id="rapportsDiv" style="position:relative"><img src="<c:url value="/images/loading.gif"/>"/></div>

<!-- Debut Liens de parentés -->
<authz:authorize ifAnyGranted="ROLE_VISU_ALL">
	<div id="parentesDiv" style="position:relative"></div>
</authz:authorize>

<!-- Debut Etablissements -->
<div id="etablissementsDiv" style="position:relative"></div>

<!-- Debut Débiteurs IS -->
<div id="debiteursDiv" style="position:relative"></div>

<script>
	// chargement Ajax des rapports-entre-tiers
	$(function() {
		DossiersApparentes.loadRapports(1);
		<authz:authorize ifAnyGranted="ROLE_VISU_ALL">
			DossiersApparentes.loadParentes();
		</authz:authorize>
        DossiersApparentes.loadDebiteurs();
        <c:if test="${command.tiersGeneral.natureTiers == 'Entreprise' || command.tiersGeneral.natureTiers == 'Etablissement'}">
            DossiersApparentes.loadEtablissements(1);
        </c:if>
	});

    var DossiersApparentes = {

	    loadRapports: function(page) {
            $('#rapportsSpinner').show();
            var showHisto = '${command.rapportsEntreTiersHisto}';
            var type = $('#typeRapportId').val();
            var sortField = $('#rapportSortField').val() || 'dateDebut';
            var sortOrder = $('#rapportSortOrder').val() || 'DESC';

            // get the data
            var params = '&page=' + page + '&showHisto=' + showHisto + (type ? '&type=' + type : '') + '&sortField=' + sortField + '&sortOrder=' + sortOrder;
            $.get('<c:url value="/rapport/rapports.do?tiers=${command.tiersGeneral.numero}"/>' + params + '&' + new Date().getTime(),
                function(rapportsPage) {
                    var html = '<fieldset>\n';
                    html += '<legend><span><fmt:message key="label.dossiers.apparentes" /></span></legend>\n';
                    html += '<div id="rapportsSpinner" style="position:absolute;right:1.5em;width:24px;display:none"><img src="<c:url value="/images/loading.gif"/>"/></div>';
                    html += DossiersApparentes.buildRapportsOptions(rapportsPage.page, rapportsPage.showHisto, rapportsPage.typeRapport, rapportsPage.typesRapportEntreTiers, rapportsPage.sortField, rapportsPage.sortOrder);
                    if (rapportsPage.totalCount > 0) {
                        html += DossiersApparentes.buildRapportsPagination(rapportsPage.page, 10, rapportsPage.totalCount);
                        html += DossiersApparentes.buildRapportsTable(rapportsPage.rapports, 'ret-', true) + '\n';
                    }
                    else {
                        html += DossiersApparentes.escape("<fmt:message key="label.dossiers.apparentes.vide"/>");
                    }
                    html += '</fieldset>\n';
                    $('#rapportsDiv').html(html);
                    Tooltips.activate_static_tooltips($('#rapportsDiv'));
                }, 'json')
                .error(Ajax.popupErrorHandler);
            return false;
        },

	    buildRapportsOptions: function(page, showHisto, typeSelectionne, typesRapportEntreTiers, sortField, sortOrder) {
            var html = '<table><tr>\n';
            html += '<td width="25%"><input class="noprint" type="checkbox" id="isRapportHisto"' + (showHisto ? ' checked="true"' : '') + ' onclick="window.location.href = App.toggleBooleanParam(window.location, \'rapportsEntreTiersHisto\', true);"> ';
            html += '<label class="noprint" for="isRapportHisto"><fmt:message key="label.historique"/></label></td>\n';
            html += '<td width="75%">&nbsp;</td>\n';
            html += '</tr><tr>\n';
            html += '<td width="25%">Type de rapport entre tiers&nbsp;:</td>\n';
            html += '<td width="75%"><form name="form" id="form"><select name="typeRapport" id="typeRapportId" onchange="return DossiersApparentes.loadRapports(' + page + ');">\n';
            html += '<option value="">Tous</option>\n';

            for (i in typesRapportEntreTiers) {
                var type = typesRapportEntreTiers[i];
                html += '<option value="' + i + '"' + (i == typeSelectionne ? ' selected' : '') + '>' + DossiersApparentes.escape(type) + '</option>\n';
            }

            html += '</select></form></td>\n';
            html += '</tr></tbody></table>\n';
            html += '<input type="hidden" id="rapportSortField" value="' + sortField +'"/>';
            html += '<input type="hidden" id="rapportSortOrder" value="' + sortOrder +'"/>';
            html += '<input type="hidden" id="rapportCurrentPage" value="' + page +'"/>\n';
            return html;
        },

	    buildRapportsPagination: function(page, pageSize, totalCount) {
            return DisplayTable.buildPagination(page, pageSize, totalCount, function(i) {
                return 'DossiersApparentes.loadRapports(' + i + ')';
            });
        },

	    sortRapportBy: function(field) {
            var current = $('#rapportSortField').val();
            if (field == current) {
                DossiersApparentes.toogleSortOrder($('#rapportSortOrder'));
            }
            else {
                $('#rapportSortField').val(field);
                $('#rapportSortOrder').val('ASC');
            }
            var page = $('#rapportCurrentPage').val();
            DossiersApparentes.loadRapports(page);
        },

	    toogleSortOrder: function(input) {
            if ($(input).val() == 'ASC') {
                $(input).val('DESC');
            }
            else {
                $(input).val('ASC');
            }
        },

	    buildRapportsTable: function(rapports, tooltipIdPrefix, sortable) {

            var hasExtensionExecutionForcee = false;
            var hasAutoriteTutelaire = false;
            var hasPrincipalCommunaute = false;
            for (var i in rapports) {
                var rapport = rapports[i];
                hasExtensionExecutionForcee = hasExtensionExecutionForcee || rapport.extensionExecutionForcee;
                hasAutoriteTutelaire = hasAutoriteTutelaire || rapport.autoriteTutelaireId;
	            hasPrincipalCommunaute = hasPrincipalCommunaute || rapport.principalCommunaute;
            }

            var html = '<table id="rapport" class="display"><thead><tr>\n';
		    if (sortable) {
                html += '<th class="sortable"><a href="#" onclick="return DossiersApparentes.sortRapportBy(\'class\');">Rapport avec le tiers</a></th>';
	            html += '<th class="sortable"><a href="#" onclick="return DossiersApparentes.sortRapportBy(\'dateDebut\');">Date début</a></th>';
	            html += '<th class="sortable"><a href="#" onclick="return DossiersApparentes.sortRapportBy(\'dateFin\');">Date fin</a></th>';
	            html += '<th class="sortable"><a href="#" onclick="return DossiersApparentes.sortRapportBy(\'tiersId\');">N° de tiers</a></th>';
	            html += '<th>Nom / Raison sociale</th>';
	            if (hasAutoriteTutelaire) {
	                html += '<th class="sortable"><a href="#" onclick="return DossiersApparentes.sortRapportBy(\'autoriteTutelaire\');">Autorité tutelaire</a></th>';
	            }
	            if (hasExtensionExecutionForcee) {
	                html += '<th class="sortable"><a href="#" onclick="return DossiersApparentes.sortRapportBy(\'extensionExecutionForcee\');">Extension à l\'exécution forcée</a></th>';
	            }
	            if (hasPrincipalCommunaute) {
		            html += '<th class="sortable"><a href="#" onclick="return DossiersApparentes.sortRapportBy(\'principalCommunaute\');">Principal de communauté</a></th>';
	            }
	        }
		    else {
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
			    if (hasPrincipalCommunaute) {
				    html += '<th>Principal de communauté</th>';
			    }
		    }
            html += '<th></th>';
            html += '</tr></thead>\n';

            html += '<tbody>\n';

            for (var i in rapports) {
                var rapport = rapports[i];
                html += '<tr class="' + (i % 2 == 0 ? 'odd' : 'even') + (rapport.annule ? ' strike' : '') +'">';

                html += '<td>' + DossiersApparentes.escape(rapport.type);
                if (rapport.toolTipMessage) {
                    var filId = tooltipIdPrefix + i;
                    html += ' <a href="#tooltip" class="staticTip" id="' + filId +'">?</a><div id="' + filId + '-tooltip" style="display:none;">' + rapport.toolTipMessage + '</div></td>';
                }
                html += '</td>';

                html += '<td>' + DossiersApparentes.escape(rapport.dateDebut) + '</td>';
                html += '<td>' + DossiersApparentes.escape(rapport.dateFin) + '</td>';
                html += '<td>' + Tiers.linkTo(rapport.numeroAutreTiers) + '</td>';

                html += '<td>';
                if (rapport.nomCourrier) {
                    var first = true;
                    for (var line in rapport.nomCourrier) {
                        if (!first) {
                            html += '<br/>';
                        }
                        html += DossiersApparentes.escape(rapport.nomCourrier[line]);
                        first = false;
                    }
                }
                html += '</td>';

                if (hasAutoriteTutelaire) {
                    html += '<td>';
                    if (rapport.nomAutoriteTutelaire) {
                        html += DossiersApparentes.escape(rapport.nomAutoriteTutelaire);
                    }
                    html += '</td>';
                }
                if (hasExtensionExecutionForcee) {
                    html += '<td>';
                    if (rapport.extensionExecutionForcee) {
                        html += '<input type="checkbox"' + (rapport.extensionExecutionForcee ? ' checked="true"' : '') + ' disabled="true"/>';
                    }
                    html += '</td>';
                }
                if (hasPrincipalCommunaute) {
                    html += '<td>';
                    if (rapport.principalCommunaute) {
                        html += '<input type="checkbox"' + (rapport.principalCommunaute ? ' checked="true"' : '') + ' disabled="true"/>';
                    }
                    html += '</td>';
                }

                html += '<td><a href="#" class="consult" title="Consultation des logs" onclick="return Dialog.open_consulter_log(\'RapportEntreTiers\', ' + rapport.id + ');">&nbsp;</a></td>';
                html += '</tr>\n';
            }
            html +='</table>\n';
            return html;
        },

		<authz:authorize ifAnyGranted="ROLE_VISU_ALL">
		    loadParentes: function() {
			    // get the data
			    $.get('<c:url value="/rapport/parentes.do?tiers=${command.tiersGeneral.numero}"/>' + '&' + new Date().getTime(), function(parentes) {
				    var html = '';
				    if (typeof parentes === 'string') {
					    // on a reçu une erreur
					    html += '<fieldset>\n';
					    html += '<legend><span><fmt:message key="label.parentes" /></span></legend>\n';
					    html += '<div class="flash-warning">' + DossiersApparentes.escape("<fmt:message key="label.affichage.parentes.impossible"/>") + '<br/><i>' + DossiersApparentes.escape(parentes) + '</i></div>\n';
					    html += '</fieldset>\n'
				    }
				    else {
					    // on a bien reçu les liens de parenté
					    if (parentes.totalCount > 0) {
						    html += '<fieldset>\n';
						    html += '<legend><span><fmt:message key="label.parentes" /></span></legend>\n';
						    html += DossiersApparentes.buildRapportsTable(parentes.rapports, 'prnt-', false) + '\n';
						    html += '</fieldset>\n'
					    }
				    }
				    $('#parentesDiv').html(html);
				    Tooltips.activate_static_tooltips($('#parentesDiv'));
			    }, 'json')
				.error(Ajax.popupErrorHandler);
			    return false;
		    },
		</authz:authorize>

	    loadDebiteurs: function() {
            // get the data
            $.get('<c:url value="/rapport/debiteurs.do?tiers=${command.tiersGeneral.numero}"/>' + '&' + new Date().getTime(), function(debiteurs) {
                var html = '';
                if (debiteurs.length) {
                    html += '<fieldset>\n';
                    html += '<legend><span><fmt:message key="label.debiteur.is" /></span></legend>\n';
                    html += DossiersApparentes.buildTableDebiteurs(debiteurs) + '\n';
                    html += '</fieldset>\n'
                }
                $('#debiteursDiv').html(html);
                Tooltips.activate_static_tooltips($('#debiteursDiv'));
            }, 'json')
            .error(Ajax.popupErrorHandler);
            return false;
        },

	    buildTableDebiteurs: function(debiteurs) {

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
                html += '<tr class="' + (i % 2 == 0 ? 'odd' : 'even') + (debiteur.annule ? ' strike' : '') + '">';
                html += '<td>' + Tiers.linkTo(debiteur.numero) + '</td>';
                html += '<td>';
                if (debiteur.nomCourrier != null) {
                    var first = true;
                    for (var line in debiteur.nomCourrier) {
                        if (!first) {
                            html += '<br/>';
                        }
                        html += DossiersApparentes.escape(debiteur.nomCourrier[line]);
                        first = false;
                    }
                }
                if (debiteur.complementNom) {
                    html += '<br/>' + DossiersApparentes.escape(debiteur.complementNom);
                }
                html += '<td>' + DossiersApparentes.escape(debiteur.nomCategorie) + '</td>';
                html += '<td>' + DossiersApparentes.escape(debiteur.personneContact) + '</td>';
                html += '<td><a href="#" class="consult" title="Consultation des logs" onclick="return Dialog.open_consulter_log(\'RapportEntreTiers\', ' + debiteur.id + ');">&nbsp;</a></td>';
                html += '</tr>\n';
            }

            return html;
        },

        loadEtablissements: function(page) {
            $('#etablissementsSpinner').show();
            var showHisto = '${command.etablissementsHisto}';
            var sortField = $('#etablissementSortField').val() || '';
            var sortOrder = $('#etablissementSortOrder').val() || '';

            // get the data
            var params = '&page=' + page + '&showHisto=' + showHisto + '&sortField=' + sortField + '&sortOrder=' + sortOrder;
            $.get('<c:url value="/rapport/etablissements.do?tiers=${command.tiersGeneral.numero}"/>' + params + '&' + new Date().getTime(),
                function(etablissementsPage) {
                    var html = '';
                    html += '<fieldset>\n';
                    html += '<legend><span><fmt:message key="label.etablissements" /></span></legend>\n';
                    html += '<div id="etablissementsSpinner" style="position:absolute;right:1.5em;width:24px;display:none"><img src="<c:url value="/images/loading.gif"/>"/></div>';
                    html += DossiersApparentes.buildEtablissementsOptions(etablissementsPage.page, etablissementsPage.showHisto, etablissementsPage.sortField, etablissementsPage.sortOrder);
                    html += DossiersApparentes.buildEtablissementsPagination(etablissementsPage.page, 10, etablissementsPage.totalCount);
                    if (etablissementsPage.totalCount > 0) {
                        html += DossiersApparentes.buildEtablissementsTable(etablissementsPage.rapports, 'etb-', true) + '\n';
                    }
                    html += '</fieldset>\n';
                    $('#etablissementsDiv').html(html);
                    Tooltips.activate_static_tooltips($('#etablissementsDiv'));
                }, 'json')
            .error(Ajax.popupErrorHandler);
            return false;
        },

        buildEtablissementsOptions: function(page, showHisto, sortField, sortOrder) {
            var html = '<table><tr>\n';
            html += '<td width="25%"><input class="noprint" type="checkbox" id="isEtablissementHisto"' + (showHisto ? ' checked="true"' : '') + ' onclick="window.location.href = App.toggleBooleanParam(window.location, \'etablissementsHisto\', true);"> ';
            html += '<label class="noprint" for="isEtablissementHisto"><fmt:message key="label.historique"/></label></td>\n';
            html += '<td width="75%">&nbsp;</td>\n';
            html += '</tr></tbody></table>\n';
            html += '<input type="hidden" id="etablissementSortField" value="' + sortField +'"/>';
            html += '<input type="hidden" id="etablissementSortOrder" value="' + sortOrder +'"/>';
            html += '<input type="hidden" id="etablissementCurrentPage" value="' + page +'"/>\n';
            return html;
        },

        buildEtablissementsPagination: function(page, pageSize, totalCount) {
            return DisplayTable.buildPagination(page, pageSize, totalCount, function(i) {
                return 'DossiersApparentes.loadEtablissements(' + i + ')';
            });
        },

        sortEtablissementBy: function(field) {
            var current = $('#etablissementSortField').val();
            if (field == current) {
                DossiersApparentes.toogleSortOrder($('#etablissementSortOrder'));
            }
            else {
                $('#etablissementSortField').val(field);
                $('#etablissementSortOrder').val('ASC');
            }
            var page = $('#etablissementCurrentPage').val();
            DossiersApparentes.loadEtablissements(page);
        },

        buildEtablissementsTable: function(etablissements, tooltipIdPrefix, sortable) {

            var html = '<table id="etablissement" class="display"><thead><tr>\n';
            if (sortable) {
                html += '<th class="sortable"><a href="#" onclick="return DossiersApparentes.sortEtablissementBy(\'class\');">Rapport avec le tiers</a></th>';
                html += '<th class="sortable"><a href="#" onclick="return DossiersApparentes.sortEtablissementBy(\'dateDebut\');">Date début</a></th>';
                html += '<th class="sortable"><a href="#" onclick="return DossiersApparentes.sortEtablissementBy(\'dateFin\');">Date fin</a></th>';
                html += '<th class="sortable"><a href="#" onclick="return DossiersApparentes.sortEtablissementBy(\'tiersId\');">N° de tiers</a></th>';
                html += '<th>Nom / Raison sociale</th>';
            }
            else {
                html += '<th>Rapport avec le tiers</th>';
                html += '<th>Date début</th>';
                html += '<th>Date fin</th>';
                html += '<th>N° de tiers</th>';
                html += '<th>Nom / Raison sociale</th>';
            }
            html += '<th></th>';
            html += '</tr></thead>\n';

            html += '<tbody>\n';

            for (var i in etablissements) {
                var etablissement = etablissements[i];
                html += '<tr class="' + (i % 2 == 0 ? 'odd' : 'even') + (etablissement.annule ? ' strike' : '') +'">';

                html += '<td>' + DossiersApparentes.escape(etablissement.type);
                if (etablissement.toolTipMessage) {
                    var filId = tooltipIdPrefix + i;
                    html += ' <a href="#tooltip" class="staticTip" id="' + filId +'">?</a><div id="' + filId + '-tooltip" style="display:none;">' + etablissement.toolTipMessage + '</div></td>';
                }
                html += '</td>';

                html += '<td>' + DossiersApparentes.escape(etablissement.dateDebut) + '</td>';
                html += '<td>' + DossiersApparentes.escape(etablissement.dateFin) + '</td>';
                html += '<td>' + Tiers.linkTo(etablissement.numeroAutreTiers) + '</td>';

                html += '<td>';
                if (etablissement.nomCourrier != null) {
                    var first = true;
                    for (var line in etablissement.nomCourrier) {
                        if (!first) {
                            html += '<br/>';
                        }
                        html += DossiersApparentes.escape(etablissement.nomCourrier[line]);
                        first = false;
                    }
                }
                html += '</td>';

                html += '<td><a href="#" class="consult" title="Consultation des logs" onclick="return Dialog.open_consulter_log(\'RapportEntreTiers\', ' + etablissement.id + ');">&nbsp;</a></td>';
                html += '</tr>\n';
            }
            html +='</table>\n';
            return html;
        },

	    escape: function(value) {
            var html = '';
            if (value) {
                html += StringUtils.escapeHTML(value);
            }
            return html;
        }
    }

</script>

<!-- Fin Dossiers Apparentes -->
