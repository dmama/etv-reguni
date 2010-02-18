<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.mouvements.dossiers.masse.traiter.full"/></tiles:put>
  	<tiles:put name="body">
		<c:set var="ligneTableau" value="${1}" scope="request" />
	    <c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
	    <form:form method="post" id="formRechercherMouvementsMasse" action="traiter.do">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors cssClass="error"/>
				<jsp:include page="form.jsp"/>
			</fieldset>

            <input type="hidden" name="retireMvtId" value=""/>
            <input type="hidden" name="reinitMvtId" value=""/>

            <script type="text/javascript">
                function reinitMouvementDossierMasse(id) {
                    var form = document.forms["formRechercherMouvementsMasse"];
                    form.reinitMvtId.value = id;
                    form.submit();
                }

                function annulerMouvementDossierMasse(id) {
                    var form = document.forms["formRechercherMouvementsMasse"];
                    form.retireMvtId.value = id;
                    form.submit();
                }

                function selectAllMouvements(checkSelectAll) {
                    var lignesMvts = document.getElementById('mvt').getElementsByTagName('tr');
                    var taille = lignesMvts.length;
                    for(var i=1; i < taille; i++) {
                        if (E$('tabIdsMvts_' + i) != null && !E$('tabIdsMvts_' + i).disabled) {
                            E$('tabIdsMvts_' + i).checked = checkSelectAll.checked;
                        }
                    }
                }

                function confirmeInclusionBordereau() {
                    var lignesMvts = document.getElementById('mvt').getElementsByTagName('tr');
                    var taille = lignesMvts.length;
                    var nbSelectionnes = 0;
                    for(var i=1; i < taille; i++) {
                        if (E$('tabIdsMvts_' + i) != null && !E$('tabIdsMvts_' + i).disabled && E$('tabIdsMvts_' + i).checked) {
                            ++ nbSelectionnes;
                        }
                    }
                    if (nbSelectionnes == 0) {
                        alert('Veuillez sélectionner au moins un mouvement à inclure');
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
                        return confirm('Voulez-vous inclure ' + txtMvt + ' dans un bordereau ?');
                    }
                }
            </script>

			<display:table name="command.results" id="mvt" pagesize="25" requestURI="/mouvement/traiter.do" class="display_table" sort="external" size="command.resultSize" partialList="true" >
				<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.aucun.mouvement.trouve" /></span></display:setProperty>
				<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.mouvement.trouve" /></span></display:setProperty>
				<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.mouvements.trouves" /></span></display:setProperty>
				<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.mouvements.trouves" /></span></display:setProperty>

                <display:column title="<input type='checkbox' name='selectAll' onclick='selectAllMouvements(this);'/>">
                    <c:if test="${!mvt.annule && mvt.etatMouvement == 'A_TRAITER'}">
                        <input type="checkbox" name="tabIdsMvts" id="tabIdsMvts_${mvt_rowNum}" value="${mvt.id}" >
                    </c:if>
                    <c:if test="${!mvt.annule && mvt.etatMouvement == 'A_ENVOYER'}">
                        <input type="checkbox" name="tabIdsMvts" id="tabIdsMvts_${mvt_rowNum}" value="" checked="true" disabled="true">
                    </c:if>
                </display:column>

				<display:column sortable="true" titleKey="label.numero.tiers" sortName="contribuable.numero">
					<c:if test="${mvt.annule}"><strike></c:if>
						<unireg:numCTB numero="${mvt.contribuable.numero}" />
					<c:if test="${mvt.annule}"></strike></c:if>
				</display:column>
				<display:column titleKey="label.nom.raison" >
					<c:if test="${mvt.annule}"><strike></c:if>
						<c:forEach var="ligne" items="${mvt.contribuable.adresseEnvoi.nomPrenom}" varStatus="rowCounter">
                            <c:if test="${rowCounter.count > 1}"><br/></c:if><c:out value="${ligne}"/>
						</c:forEach>
					<c:if test="${mvt.annule}"></strike></c:if>
				</display:column>
				<display:column titleKey="label.type.mouvement" >
					<c:if test="${mvt.annule}"><strike></c:if>
					    <fmt:message key="option.type.mouvement.${mvt.typeMouvement}"/>
					<c:if test="${mvt.annule}"></strike></c:if>
				</display:column>
				<display:column sortable="true" titleKey="label.etat.mouvement" sortName="etat">
					<c:if test="${mvt.annule}"><strike></c:if>
					    <fmt:message key="option.etat.mouvement.${mvt.etatMouvement}"/>
					<c:if test="${mvt.annule}"></strike></c:if>
				</display:column>
				<c:if test="${command.montreInitiateur}">
                    <display:column titleKey="label.collectivite.administrative">
                        <c:if test="${mvt.annule}"><strike></c:if>
                            <c:out value="${mvt.collectiviteAdministrative}"/>
                        <c:if test="${mvt.annule}"></strike></c:if>
                    </display:column>
                </c:if>
				<display:column titleKey="label.destination" >
					<c:if test="${mvt.annule}"><strike></c:if>
						<c:out value="${mvt.destinationUtilisateur}" />
					<c:if test="${mvt.annule}"></strike></c:if>
				</display:column>

                <display:column style="action">
                    <a href="../tiers/mouvement.do?height=360&width=900&idMvt=${mvt.id}&TB_iframe=true&modal=true" class="detail thickbox" title="Détail d'un mouvement">&nbsp;</a>
                    <unireg:raccourciConsulter link="../common/consult-log.do?height=200&width=800&nature=MouvementDossier&id=${mvt.id}&TB_iframe=true&modal=true" thickbox="true" tooltip="Edition des logs"/>
                    <c:if test="${mvt.etatMouvement == 'A_TRAITER'}">
                        <unireg:raccourciAnnuler onClick="annulerMouvementDossierMasse(${mvt.id});" tooltip="Annuler un mouvement"/>
                    </c:if>
                    <c:if test="${mvt.etatMouvement == 'A_ENVOYER' || mvt.etatMouvement == 'RETIRE'}">
                        <unireg:raccourciReinit onClick="reinitMouvementDossierMasse(${mvt.id});" tooltip="Ré-initialiser un mouvement"/>
                    </c:if>
                </display:column>

			</display:table>

            <table border="0">
                <c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
                <tr class="<c:if test="${(ligneTableau % 2) == 0 }">even</c:if><c:if test="${ligneTableau % 2 == 1}">odd</c:if>" >
                    <td width="25%">&nbsp;</td>
                    <td width="50%">
                        <div id="boutonInclusion" class="navigation-action">
                            <input type="submit" value="<fmt:message key='label.bouton.inclure.dans.bordereau'/>" name="inclure" onclick="return confirmeInclusionBordereau();"/>
                        </div>
                        &nbsp;
                    </td>
                    <td width="25%">&nbsp;</td>
                </tr>
            </table>

            <script type="text/javascript">
                var lignesMvts = document.getElementById('mvt').getElementsByTagName('tr');
                var taille = lignesMvts.length;
                var nbSelectionnables = 0;
                for(var i=1; i < taille; i++) {
                    if (E$('tabIdsMvts_' + i) != null && !E$('tabIdsMvts_' + i).disabled) {
                        ++ nbSelectionnables;
                    }
                }
                if (nbSelectionnables == 0) {
                    document.getElementById('boutonInclusion').style.display = "none";
                }
            </script>

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