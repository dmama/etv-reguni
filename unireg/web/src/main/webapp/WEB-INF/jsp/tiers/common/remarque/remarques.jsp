<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp"%>

<c:set var="tiersId" value="${param.tiersId}"/>
<c:set var="printview" value="${param.printview}"/>

<!-- Debut Remarques -->
<unireg:setAuth var="autorisations" tiersId="${command.tiers.numero}"/>

<div id="remarques">

    <c:if test="${autorisations.remarques}">
        <a id="addRemarque" class="add noprint" href="#">Ajouter une remarque</a>
        <div id="newRemarque" class="new_remarque" style="display:none;">
            <textarea cols="80" rows="3"></textarea><br>
            <input type="button" value="Ajouter"/>&nbsp;ou&nbsp;<a href="#">annuler</a>
        </div>
    </c:if>

    <div id="remarquesDiv">
        <img src="<c:url value="/images/loading.gif"/>"/>
    </div>

</div>

<script>
	// chargement Ajax des remarques
	$(function() {
		Remarques.refreshRemarques();
	});

    var Remarques = {

        refreshRemarques: function() {
            <c:if test="${autorisations.remarques}">
                $('#addRemarque').show();
                var newRemarque = $('#newRemarque');
                newRemarque.hide();
                newRemarque.find('textarea').val('');
            </c:if>
            Remarques.loadRemarques(1);
        },

	    loadRemarques: function(page) {
            $('#remarquesSpinner').show();
            var showHisto = '${command.remarquesHisto}';

            // get the data
            var params = '&page=' + page + '&showHisto=' + showHisto<c:if test="${printview}"> + '&pageSize=0'</c:if>;
            $.get('<c:url value="/remarque/list.do?tiersId="/>${tiersId}' + params + '&' + new Date().getTime(),
                function(remarquesPage) {
                    var html = '<fieldset>\n';
                    html += '<legend><span><fmt:message key="label.remarques" /></span></legend>\n';
                    html += '<div id="remarquesSpinner" style="position:absolute;right:1.5em;width:24px;display:none"><img src="<c:url value="/images/loading.gif"/>"/></div>';
                    html += Remarques.buildRemarquesOptions(remarquesPage.page, remarquesPage.showHisto);
                    if (remarquesPage.totalCount > 0) {
                        $('#remarqueTabAnchor').text('Remarques (' + remarquesPage.totalCount + ')');

                        <c:choose>
                        <c:when test="${printview}">const pageSize = 0;</c:when>
                        <c:otherwise>const pageSize = 20;</c:otherwise>
                        </c:choose>
                        html += Remarques.buildRemarquesPagination(remarquesPage.page, pageSize, remarquesPage.totalCount);
                        html += Remarques.buildRemarquesTable(remarquesPage.remarques) + '\n';
                    }
                    else {
                        $('#remarqueTabAnchor').text('Remarques');
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
            html += '<td width="25%"><input class="noprint" type="checkbox" id="isRemarqueHisto"' + (showHisto ? ' checked="true"' : '') + ' onclick="window.location.href = App.toggleBooleanParam(window.location, \'remarquesHisto\', true);"> ';
            html += '<label class="noprint" for="isRemarqueHisto"><fmt:message key="label.historique"/></label></td>\n';
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

            var html = '<table id="remarque" class="remarques" cellspacing="0" border="0">\n';

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
                    <c:choose>
                    <c:when test="${printview}">
                        html += '<div id="rq-long-' + i + '">' + remarque.htmlText + '</div>';
                    </c:when>
                    <c:otherwise>
                        html += '<div id="rq-short-' + i + '">' + remarque.shortHtmlText + Remarques.buildToggle(i) + '</div>';
                        html += '<div id="rq-long-' + i + '" style="display:none;">' + remarque.htmlText + '</div>';
                    </c:otherwise>
                    </c:choose>
                    html += '</td>';
                }

                html += '<td style="width:2em;">';
                <c:if test="${autorisations.remarques}">
                    if (!remarque.annule) {
                        html += '<a class="delete"';
                        html += ' onclick="if (!confirm(\'Voulez-vous vraiment annuler cette remarque ?\')) return false;';
                        html += 'Form.dynamicSubmit(\'POST\',\'<c:url value="/remarque/cancel.do"/>\',{\'remarqueId\':\'' + remarque.id + '\'}); return false;"';
                        html += ' title="Annulation de remarque" href="<c:url value="/remarque/cancel.do"/>"></a>&nbsp;';
                    }
                </c:if>
                html += '<a href="#" class="consult" title="Consultation des logs" onclick="return Dialog.open_consulter_log(\'Remarque\', ' + remarque.id + ');">&nbsp;</a></td>';
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
    };

    <c:if test="${autorisations.remarques}">

        $('#addRemarque').click(function() {
            $('#addRemarque').hide();
            $('#newRemarque').show();
            return false;
        });

        $('#newRemarque').find('input').click(function() {
            var text = $('#newRemarque').find('textarea').val();
            $.post('<c:url value="/remarque/add.do"/>', {'tiersId': ${tiersId}, 'text': text}, function() {
                // on success, refresh all
                Remarques.refreshRemarques();
            });
            return false;
        });

        $('#newRemarque').find('a').click(function() {
            $('#addRemarque').show();
            $('#newRemarque').hide();
            return false;
        });

    </c:if>

</script>

<!-- Fin Remaques -->
