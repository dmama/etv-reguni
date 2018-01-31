<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.mouvements.dossiers.masse.imprimer.bordereaux.full"/></tiles:put>
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>

        <form:form method="post" id="formImprimerBordereauxMouvements" action="imprimer-nouveau-bordereau.do">

        	<input type="hidden" name="src" value="${src}"/>
        	<input type="hidden" name="dest" value="${dest}"/>
        	<input type="hidden" name="type" value="${type}"/>

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
                    	var sel = $('#selection_' + i);
                        if (sel && !sel.attr('disabled')) {
                            sel.attr('checked', checkSelectAll.checked);
                        }
                    }
                }

                function confirmeImpressionBordereau() {
                    var lignesMvts = document.getElementById('mvt').getElementsByTagName('tr');
                    var taille = lignesMvts.length;
                    var nbSelectionnes = 0;
                    for(var i=1; i < taille; i++) {
                    	var sel = $('#selection_' + i);
                        if (sel && !sel.attr('disabled') && sel.attr('checked')) {
                            ++ nbSelectionnes;
                        }
                    }
                    if (nbSelectionnes == 0) {
                        alert('Veuillez selectionner au moins un dossier à inclure');
                        return false;
                    }
                    else {
                        var txtMvt;
                        if (nbSelectionnes == 1) {
                            txtMvt = 'le dossier sélectionné, pour lequel';
                        }
                        else {
                            txtMvt = 'les ' + nbSelectionnes + ' dossiers sélectionnés, pour lesquels';
                        }
                        if (confirm('Voulez-vous imprimer le bordereau avec ' + txtMvt + ' un mouvement sera alors définitivement créé ?')) {
                            var printButton = document.getElementById('printButton');
                            printButton.style.display = "none";
                            var closeButton = document.getElementById('closeButton');
                            closeButton.value = "Fermer";
                            var globalErrorDiv = document.getElementById('globalErrors');
                            globalErrorDiv.style.display = "none";
                            return true;
                        }
                        else {
                            return false;
                        }
                    }
                }

            </script>

            <display:table name="command.mouvements" id="mvt" pagesize="1000" requestURI="/mouvement/bordereau/detail-avant-impression.do" class="display_table" sort="list">

				<c:set var="selectable" value="${mvt.contribuable.nomCommuneGestion != null}"/>
                <display:column title="<input type='checkbox' id='selection_all' name='selectAll' onclick='selectAllMouvements(this);' checked='true'/>">
                	<c:if test="${selectable}">
	                    <input type="checkbox" name="selection" id="selection_${mvt_rowNum}" value="${mvt.id}" checked="true"/>
	                </c:if>
                </display:column>
                <display:column sortable="true" titleKey="label.numero.contribuable">
                    <unireg:numCTB numero="${mvt.contribuable.numero}"/>
                </display:column>
				<display:column titleKey="label.nom.raison" >
					<c:if test="${mvt.annule}"><strike></c:if>
						<c:forEach var="ligne" items="${mvt.contribuable.nomPrenom}" varStatus="rowCounter">
                            <c:if test="${rowCounter.count > 1}"><br/></c:if><c:out value="${ligne}"/>
						</c:forEach>
					<c:if test="${mvt.annule}"></strike></c:if>
				</display:column>
				<display:column titleKey="label.commune.fraction" style="width: 35%;">
					<c:if test="${selectable}">
				    	<c:out value="${mvt.contribuable.nomCommuneGestion}"/>
				    </c:if>
					<c:if test="${!selectable}">
				    	<span class="error"><fmt:message key="error.pas.oid.gestion.impression.bordereau.impossible"/></span>
				    </c:if>
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
                            <input type="button" id="closeButton" value="Retour" onclick="document.location.href='a-imprimer.do'"/>
                        </div>
                    </td>
                    <td width="25%">&nbsp;</td>
                </tr>
            </table>

        </form:form>

   </tiles:put>

</tiles:insert>