<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<c:set var="tiersId" value="${param.tiersId}"/>

<!-- Debut Remarques -->
<unireg:setAuth var="autorisations" tiersId="${command.tiers.numero}"/>

<div id="remarques">

    <authz:authorize ifAnyGranted="ROLE_REMARQUE_TIERS">
        <a id="addRemarque" class="add noprint" href="#">Ajouter une remarque</a>
        <div id="newRemarque" class="new_remarque" style="display:none;">
            <textarea cols="80" rows="3"></textarea><br>
            <input type="button" value="Ajouter"/>&nbsp;ou&nbsp;<a href="#">annuler</a>
        </div>
    </authz:authorize>

    <div id="remarquesDiv">
        <img src="<c:url value="/images/loading.gif"/>"/>
    </div>

</div>

<script>
	// chargement Ajax des remarques
	$(function() {
		Remarques.loadRemarques(1);

        $('#addRemarque').show();
        var newRemarque = $('#newRemarque');
        newRemarque.hide();
        newRemarque.find('textarea').val('');
	});

    var Remarques = {

	    loadRemarques: function(page) {
            $('#remarquesSpinner').show();
            var showHisto = $('#isRemarqueHisto').attr('checked') ? 'true' : 'false';

            // get the data
            var params = '&page=' + page + '&showHisto=' + showHisto;
            $.get('<c:url value="/remarque/list.do?tiersId="/>${tiersId}' + params + '&' + new Date().getTime(),
                function(remarquesPage) {
                    var html = '<fieldset>\n';
                    html += '<legend><span><fmt:message key="label.remarques" /></span></legend>\n';
                    html += '<div id="remarquesSpinner" style="position:absolute;right:1.5em;width:24px;display:none"><img src="<c:url value="/images/loading.gif"/>"/></div>';
                    html += Remarques.buildRemarquesOptions(remarquesPage.page, remarquesPage.showHisto);
                    if (remarquesPage.totalCount > 0) {
                        html += Remarques.buildRemarquesPagination(remarquesPage.page, 20, remarquesPage.totalCount);
                        html += Remarques.buildRemarquesTable(remarquesPage.remarques) + '\n';
                    }
                    else {
                        html += Remarques.escapeHTML("<fmt:message key="label.remarques.vide"/>");
                    }
                    html += '</fieldset>\n';
                    $('#remarquesDiv').html(html);
                    Tooltips.activate_static_tooltips($('#remarquesDiv'));
                }, 'json')
                .error(Ajax.popupErrorHandler);
            return false;
        },

	    buildRemarquesOptions: function(page, showHisto) {
            var html = '<table><tr>\n';
            html += '<td width="25%"><input class="noprint" type="checkbox" id="isRemarqueHisto"' + (showHisto ? ' checked="true"' : '') + ' onclick="return Remarques.loadRemarques(' + page + ');"> ';
            html += '<label class="noprint" for="isRemarqueHisto">Historique</label></td>\n';
            html += '<td width="75%">&nbsp;</td>\n';
            html += '</tr></tbody></table>\n';
            html += '<input type="hidden" id="remarqueCurrentPage" value="' + page +'"/>\n';
            return html;
        },

	    buildRemarquesPagination: function(page, pageSize, totalCount) {
            return DisplayTable.buildPagination(page, pageSize, totalCount, function(i) {
                return 'Remarques.loadRemarques(' + i + ')';
            });
        },

	    buildRemarquesTable: function(remarques) {

            var html = '<table id="remarque" class="display">\n';

            html += '<tbody>\n';

            for (var i in remarques) {
                var remarque = remarques[i];
                html += '<tr class="' + (i % 2 == 0 ? 'odd' : 'even') + (remarque.annule ? ' strike' : '') +'">';

                html += '<td class="entete">le ' + Remarques.escapeHTML(remarque.date) + ' par ' + Remarques.escapeHTML(remarque.user) + '</td>';
                if (remarque.nbLines < remarque.thresholdNbLines) {
                    html += '<td class="texte">' + remarque.htmlText + '</td>';
                }
                else {
                    html += '<td class="texte">';
                    html += '<div id="rq-short-' + i + '">' + remarque.shortHtmlText + Remarques.buildToggle(i) + '</div>';
                    html += '<div id="rq-long-' + i + '" style="display:none;">' + remarque.htmlText + '</div>';
                    html += '</td>';
                }

                html += '<td style="width:2em;">';
                if (!remarque.annule) {
                    html += '<a class="delete"';
                    html += ' onclick="if (!confirm(\'Voulez-vous vraiment annuler cette remarque ?\')) return false;';
                    html += 'Form.dynamicSubmit(\'POST\',\'<c:url value="/remarque/cancel.do"/>\',{\'remarqueId\':\'' + remarque.id + '\'}); return false;"';
                    html += ' title="Annulation de remarque" href="<c:url value="/remarque/cancel.do"/>">&nbsp;</a>';
                } else {
                    html += '&nbsp;';
                }
                html += '</td>';

                html += '<td style="width:2em;"><a href="#" class="consult" title="Consultation des logs" onclick="return Dialog.open_consulter_log(\'Remarque\', ' + remarque.id + ');">&nbsp;</a></td>';
                html += '</tr>\n';
            }
            html +='</table>\n';
            return html;
        },

        escapeHTML: function(value) {
            var html = '';
            if (value) {
                html += StringUtils.escapeHTML(value);
            }
            return html;
        },

        buildToggle: function(index) {
            return '<a title="Visualiser la fin de la remarque" class="ellipsis" href="#" onclick="Remarques.toggleLongVersion(' + index + ')">&nbsp;</a>';
        },

        toggleLongVersion: function(index) {
            $('td #rq-short-' + index).hide();
            $('td #rq-long-' + index).show();
        }
    }

    $('#addRemarque').click(function() {
        $('#addRemarque').hide();
        $('#newRemarque').show();
        return false;
    });

    $('#newRemarque').find('input').click(function() {
        var text = $('#newRemarque').find('textarea').val();
        $.post('<c:url value="/remarque/add.do"/>', {'tiersId': ${tiersId}, 'text': text}, function() {
            // on success, refresh all
            Remarques.loadRemarques(1);
        });
        return false;
    });

    $('#newRemarque').find('a').click(function() {
        $('#addRemarque').show();
        $('#newRemarque').hide();
        return false;
    });

</script>

<!-- Fin Remaques -->
