<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.mouvements.dossiers.masse.receptionner.full"/></tiles:put>
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>

        <form:form method="post" id="formReceptionnerMouvements" action="valider-reception.do">

			<%-- on mets ici dans le champ ''id'' l''identifiant technique du bordereau --%>
			<%-- afin qu''il soit disponible pour un changement de page suivant un submit --%>
			<%-- de la réception --%>
        	<input type="hidden" name="id" value="${command.id}"/>

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
						var cols = lignesMvts[i].getElementsByTagName("td");
						if (cols != null && cols.length > 1) {
							var inputs = cols[0].getElementsByTagName("input");
							if (inputs != null && inputs.length > 0) {
								var chkbox = inputs[0];
								if (chkbox != null && !chkbox.disabled && chkbox.name == 'selection') {
									chkbox.checked = checkSelectAll.checked;
								}
							}
						}
                    }
                }

                function confirmeReceptionBordereau() {
                    var lignesMvts = document.getElementById('mvt').getElementsByTagName('tr');
                    var taille = lignesMvts.length;
                    var nbSelectionnes = 0;
                    for(var i=1; i < taille; i++) {
						var cols = lignesMvts[i].getElementsByTagName("td");
						if (cols != null && cols.length > 1) {
							var inputs = cols[0].getElementsByTagName("input");
							if (inputs != null && inputs.length > 0) {
								var chkbox = inputs[0];
								if (chkbox != null && !chkbox.disabled && chkbox.name == 'selection' && chkbox.checked) {
                                	++ nbSelectionnes;
								}
							}
						}
                    }
                    if (nbSelectionnes == 0) {
                        alert('Veuillez selectionner au moins un dossier à réceptionner');
                        return false;
                    }
                    else {
                        var txtMvt;
                        if (nbSelectionnes == 1) {
                            txtMvt = 'le dossier sélectionné';
                        }
                        else {
                            txtMvt = 'les ' + nbSelectionnes + ' dossiers sélectionnés';
                        }
                        return confirm('Voulez-vous réceptionner ' + txtMvt + ' ?');
                    }
                }

            </script>

            <display:table name="command.mvts" id="mvt" pagesize="15" requestURI="/mouvement/bordereau/detail-reception.do" class="display_table" sort="list" decorator="ch.vd.uniregctb.decorator.TableEntityDecorator">

                <display:column title="<input type='checkbox' id='selection_all' name='selectAll' onclick='selectAllMouvements(this);'/>">
                    <c:if test="${mvt.etatMouvement == 'RECU_BORDEREAU' && !mvt.annule}">
                        <input type="checkbox" name="selection" id="selection_${mvt_rowNum}" value="" checked="true" disabled="true"/>
                    </c:if>
                    <c:if test="${mvt.etatMouvement == 'TRAITE' && !mvt.annule}">
                        <input type="checkbox" name="selection" id="selection_${mvt_rowNum}" value="${mvt.id}"/>
                    </c:if>
                </display:column>
                <display:column sortable="true" titleKey="label.numero.contribuable">
                    <unireg:numCTB numero="${mvt.contribuable.numero}"/>
                </display:column>
				<display:column titleKey="label.nom.raison" >
					<c:forEach var="ligne" items="${mvt.contribuable.nomPrenom}" varStatus="rowCounter">
						<c:if test="${rowCounter.count > 1}"><br/></c:if><c:out value="${ligne}"/>
					</c:forEach>
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
                        <c:if test="${command.auMoinsUnSelectionnable}">
                            <div class="navigation-action">
                                <input type="submit" id="receptButton" value="<fmt:message key='label.bouton.receptionner'/>" name="receptionner" onclick="return confirmeReceptionBordereau();"/>
                            </div>
                        </c:if>
                        &nbsp;
                    </td>
                    <td width="25%">
                        <div class="navigation-action">
							<input type="button" id="closeButton" value="Retour" onclick="document.location.href='reception.do'"/>
                        </div>
                    </td>
                    <td width="25%">&nbsp;</td>
                </tr>
            </table>

        </form:form>

   </tiles:put>

</tiles:insert>