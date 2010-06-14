<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.mouvements.dossiers.masse.consulter.full"/></tiles:put>
  	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/consult_mouvementdossier.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
	    <form:form method="post" id="formRechercherMouvementsMasse" action="consulter.do">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors cssClass="error"/>
				<jsp:include page="form.jsp"/>
			</fieldset>

			<display:table name="command.results" id="mvt" pagesize="25" requestURI="/mouvement/consulter.do" class="display_table" sort="external" size="command.resultSize" partialList="true" >
				<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.aucun.mouvement.trouve" /></span></display:setProperty>
				<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.mouvement.trouve" /></span></display:setProperty>
				<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.mouvements.trouves" /></span></display:setProperty>
				<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.mouvements.trouves" /></span></display:setProperty>

				<display:column sortable="true" titleKey="label.numero.tiers" sortName="contribuable.numero">
					<c:if test="${mvt.annule}"><strike></c:if>
						<unireg:numCTB numero="${mvt.contribuable.numero}" />
					<c:if test="${mvt.annule}"></strike></c:if>
				</display:column>
				<display:column sortable="true" titleKey="label.date.mouvement" sortName="dateMouvement">
					<c:if test="${mvt.annule}"><strike></c:if>
                        <fmt:formatDate value="${mvt.dateMouvement}" pattern="dd.MM.yyyy"/>
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
				<display:column titleKey="label.destination.utilisateur" >
					<c:if test="${mvt.annule}"><strike></c:if>
						<c:out value="${mvt.destinationUtilisateur}" />
					<c:if test="${mvt.annule}"></strike></c:if>
				</display:column>

                <display:column style="action">
                    <a href="../tiers/mouvement.do?height=360&width=900&idMvt=${mvt.id}&TB_iframe=true&modal=true" class="detail thickbox" title="Détail d'un mouvement">&nbsp;</a>
                    <unireg:raccourciConsulter link="../common/consult-log.do?height=200&width=800&nature=MouvementDossier&id=${mvt.id}&TB_iframe=true&modal=true" thickbox="true" tooltip="Edition des logs"/>			
                </display:column>

			</display:table>

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