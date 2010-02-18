<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.mouvements.dossiers.masse.imprimer.bordereaux.full"/></tiles:put>
  	<tiles:put name="body">
		<c:set var="ligneTableau" value="${1}" scope="request" />
	    <c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />

        <form:form method="post" id="formImprimerBordereauxMouvements" action="detail-bordereau.do">

            <table>
                <tr>
                    <td width="25%">&nbsp;</td>
                    <td><fmt:message key="label.bordereau.de"/>&nbsp;:</td>
                    <td><c:out value="${command.nomCollAdmInitiatrice}"/></td>
                    <td><fmt:message key="label.bordereau.vers"/>&nbsp;:</td>
                    <td>
                        <c:if test="${command.typeMouvement == 'EnvoiDossier'}">
                            <c:out value="${command.nomCollAdmDestinataire}"/>
                        </c:if>
                        <c:if test="${command.typeMouvement == 'ReceptionDossier'}">
                            <fmt:message key="label.archives"/>
                        </c:if>
                    </td>
                    <td width="25%">&nbsp;</td>
            </table>

            <script type="text/javascript">
                function selectAllMouvements(checkSelectAll) {
                    var lignesMvts = document.getElementById('mvt').getElementsByTagName('tr');
                    var taille = lignesMvts.length;
                    for(var i=1; i < taille; i++) {
                        if (E$('selection_' + i) != null && !E$('selection_' + i).disabled) {
                            E$('selection_' + i).checked = checkSelectAll.checked;
                        }
                    }
                }

                function confirmeImpressionBordereau() {
                    var lignesMvts = document.getElementById('mvt').getElementsByTagName('tr');
                    var taille = lignesMvts.length;
                    var nbSelectionnes = 0;
                    for(var i=1; i < taille; i++) {
                        if (E$('selection_' + i) != null && !E$('selection_' + i).disabled && E$('selection_' + i).checked) {
                            ++ nbSelectionnes;
                        }
                    }
                    if (nbSelectionnes == 0) {
                        alert('Veuillez selectionner au moins un mouvement à inclure');
                        return false;
                    }
                    else {
                        var txtMvt;
                        if (nbSelectionnes == 1) {
                            txtMvt = 'le mouvement sélectionné';
                        }
                        else {
                            txtMvt = 'les ' + nbSelectionnes + ' mouvements sélectionnés';
                        }
                        if (confirm('Voulez-vous imprimer le bordereau avec ' + txtMvt + ' ?')) {
                            var printButton = document.getElementById('printButton');
                            printButton.style.display = "none";
                            var closeButton = document.getElementById('closeButton');
                            closeButton.value = "Fermer";
                            return true;
                        }
                        else {
                            return false;
                        }
                    }
                }

                function termineImpressionBordereau() {
                    var closeButton = document.getElementById('closeButton');

                    // Retour est la valeur par défaut quand on arrive sur la page, avant impression
                    if (closeButton.value == 'Retour') {
                        // pas besoin de rafraîchissement de la page en dessous
                        self.parent.tb_remove();
                    }
                    else {
                        // sinon, on a changé la valeur, et il faut rafraîchir la page en dessous
                        top.location.reload(true);
                    }
                    return true;
                }

            </script>

            <display:table name="command.mouvements" id="mvt" pagesize="1000" requestURI="/mouvement/detail-bordereau.do" class="display_table" sort="list">

                <display:column title="<input type='checkbox' id='selection_all' name='selectAll' onclick='selectAllMouvements(this);' checked='true'/>">
                    <input type="checkbox" name="selection" id="selection_${mvt_rowNum}" value="${mvt.id}" checked="true"/>
                </display:column>
                <display:column sortable="true" titleKey="label.numero.contribuable">
                    <unireg:numCTB numero="${mvt.contribuable.numero}"/>
                </display:column>
				<display:column titleKey="label.nom.raison" >
					<c:if test="${mvt.annule}"><strike></c:if>
						<c:forEach var="ligne" items="${mvt.contribuable.adresseEnvoi.nomPrenom}" varStatus="rowCounter">
                            <c:if test="${rowCounter.count > 1}"><br/></c:if><c:out value="${ligne}"/>
						</c:forEach>
					<c:if test="${mvt.annule}"></strike></c:if>
				</display:column>
				<display:column titleKey="label.commune.fraction">
				    <c:out value="${mvt.contribuable.nomCommuneGestion}"/>
				</display:column>

            </display:table>

            <table border="0">
                <tr><td colspan=4/>&nbsp;</td></tr>
                <tr>
                    <td width="25%">&nbsp;</td>
                    <td width="25%">
                        <div class="navigation-action">
                            <input type="submit" id="printButton" value="<fmt:message key='label.bouton.imprimer.bordereau'/>" name="imprimer" onclick="return confirmeImpressionBordereau();"/>
                        </div>
                    </td>
                    <td width="25%">
                        <div class="navigation-action">
                            <input type="button" id="closeButton" value="Retour" onclick="return termineImpressionBordereau();"/>
                        </div>
                    </td>
                    <td width="25%">&nbsp;</td>
                </tr>
            </table>

        </form:form>

		<script type="text/javascript">
			function AppSelect_OnChange(select) {
				var value = select.options[select.selectedIndex].value;
				if ( value && value !== '') {
					//window.open(value, '_blank') ;
					window.location.href = value;
				}
			}
		</script>

   </tiles:put>

</tiles:insert>