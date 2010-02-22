<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.mouvements.dossiers.masse.receptionner.full"/></tiles:put>
  	<tiles:put name="body">
		<c:set var="ligneTableau" value="${1}" scope="request" />
	    <c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />

        <form:form method="post" id="formReceptionnerMouvements" action="detail-reception-bordereau.do">

            <table>
                <tr>
                    <td width="25%">&nbsp;</td>
                    <td><fmt:message key="label.bordereau.de"/>&nbsp;:</td>
                    <td><c:out value="${command.nomCollAdmEmettrice}"/></td>
                    <td><fmt:message key="label.bordereau.vers"/>&nbsp;:</td>
                    <td><c:out value="${command.nomCollAdmDestinataire}"/></td>
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

                function confirmeReceptionBordereau() {
                    var lignesMvts = document.getElementById('mvt').getElementsByTagName('tr');
                    var taille = lignesMvts.length;
                    var nbSelectionnes = 0;
                    for(var i=1; i < taille; i++) {
                        if (E$('selection_' + i) != null && !E$('selection_' + i).disabled && E$('selection_' + i).checked) {
                            ++ nbSelectionnes;
                        }
                    }
                    if (nbSelectionnes == 0) {
                        alert('Veuillez selectionner au moins un mouvement à réceptionner');
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
                        return confirm('Voulez-vous réceptionner ' + txtMvt + ' ?');
                    }
                }

            </script>

            <display:table name="command.mvts" id="mvt" pagesize="15" requestURI="/mouvement/detail-reception-bordereau.do" class="display_table" sort="list">

                <display:column title="<input type='checkbox' id='selection_all' name='selectAll' onclick='selectAllMouvements(this);'/>">
                    <c:if test="${mvt.etatMouvement == 'RECU_BORDEREAU' && !mvt.annule}">
                        <input type="checkbox" name="selection" id="selection_${mvt_rowNum}" value="" checked="true" disabled="true"/>
                    </c:if>
                    <c:if test="${mvt.etatMouvement == 'TRAITE' && !mvt.annule}">
                        <input type="checkbox" name="selection" id="selection_${mvt_rowNum}" value="${mvt.id}"/>
                    </c:if>
                </display:column>
                <display:column sortable="true" titleKey="label.numero.contribuable">
                    <c:if test="${mvt.annule}"><strike></c:if>
                    <unireg:numCTB numero="${mvt.contribuable.numero}"/>
                    <c:if test="${mvt.annule}"></strike></c:if>
                </display:column>
				<display:column titleKey="label.nom.raison" >
					<c:if test="${mvt.annule}"><strike></c:if>
						<c:forEach var="ligne" items="${mvt.contribuable.adresseEnvoi.nomPrenom}" varStatus="rowCounter">
                            <c:if test="${rowCounter.count > 1}"><br/></c:if><c:out value="${ligne}"/>
						</c:forEach>
					<c:if test="${mvt.annule}"></strike></c:if>
				</display:column>
				<display:column titleKey="label.commune.fraction">
                    <c:if test="${mvt.annule}"><strike></c:if>
				    <c:out value="${mvt.contribuable.nomCommuneGestion}"/>
					<c:if test="${mvt.annule}"></strike></c:if>
				</display:column>

            </display:table>

            <table border="0">
                <tr><td colspan=4/>&nbsp;</td></tr>
                <tr>
                    <td width="25%">&nbsp;</td>
                    <td width="25%">
                        <c:if test="${command.auMoinsUnSelectionnable}">
                            <div class="navigation-action">
                                <input type="submit" id="receptButton" value="<fmt:message key='label.bouton.receptionner'/>" name="receptionner" onclick="return confirmeReceptionBordereau();"/>
                            </div>
                        </c:if>
                        &nbsp;
                    </td>
                    <td width="25%">
                        <div class="navigation-action">
                            <c:if test="${command.apresReception}">
                                <input type="button" id="closeButton" value="Fermer" onclick="top.location.reload(true);"/>
                            </c:if>
                            <c:if test="${!command.apresReception}">
                                <unireg:closeOverlayButton text="Retour"/>
                            </c:if>
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