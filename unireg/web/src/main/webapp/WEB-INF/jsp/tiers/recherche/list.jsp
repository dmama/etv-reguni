<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.recherche.tiers" /></tiles:put>
  	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/recherche.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
  	<tiles:put name="body">
		<c:set var="ligneTableau" value="${1}" scope="request" />
	    <c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
	    <form:form method="post" id="formRechercheTiers" action="list.do">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors cssClass="error"/>
				<jsp:include page="form.jsp">
					<jsp:param name="typeRecherche" value="principale" />
				</jsp:include>
			</fieldset>
		
			<display:table name="list" id="tiers" pagesize="${parametresApp.nbMaxParPage}" requestURI="/tiers/list.do" class="display_table" sort="list">
				<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncun.tiers.trouve" /></span></display:setProperty>
				<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.tiers.trouve" /></span></display:setProperty>
				<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.tiers.trouves" /></span></display:setProperty>
				<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.tiers.trouves" /></span></display:setProperty>
	
				<display:column sortable ="true" titleKey="label.numero.tiers" sortProperty="numero" >
					<c:if test="${tiers.annule}"><strike></c:if>
						<a href="visu.do?id=${tiers.numero}"><unireg:numCTB numero="${tiers.numero}" /></a>
					<c:if test="${tiers.annule}"></strike></c:if>
				</display:column>
				<display:column sortable ="true" titleKey="label.role" >
					<c:if test="${tiers.annule}"><strike></c:if>
						<c:out value="${tiers.roleLigne1}" />
						<c:if test="${tiers.roleLigne2 != null}">
							<br><c:out value="${tiers.roleLigne2}" />
						</c:if>
					<c:if test="${tiers.annule}"></strike></c:if>
				</display:column>
				<display:column sortable ="true" titleKey="label.nom.raison" >
					<c:if test="${tiers.annule}"><strike></c:if>
						<c:out value="${tiers.nom1}" />
						<c:if test="${tiers.nom2 != null}">
							<br><c:out value="${tiers.nom2}" />
						</c:if>
					<c:if test="${tiers.annule}"></strike></c:if>
				</display:column>
				<display:column titleKey="label.date.naissance" sortable="true" sortName="dateNaissance" sortProperty="dateNaissance">
					<c:if test="${tiers.annule}"><strike></c:if>
						<unireg:date date="${tiers.dateNaissance}"></unireg:date>
					<c:if test="${tiers.annule}"></strike></c:if>
				</display:column>
				<display:column sortable ="true" titleKey="label.npa" >
					<c:if test="${tiers.annule}"><strike></c:if>
						<c:out value="${tiers.npa}" />
					<c:if test="${tiers.annule}"></strike></c:if>
				</display:column>
				<display:column sortable ="true" titleKey="label.localitePays" >
					<c:if test="${tiers.annule}"><strike></c:if>
						<c:out value="${tiers.localiteOuPays}" />
					<c:if test="${tiers.annule}"></strike></c:if>
				</display:column>
				<display:column sortable ="true" titleKey="label.for.principal" >
					<c:if test="${tiers.annule}"><strike></c:if>
						<c:out value="${tiers.forPrincipal}" />
					<c:if test="${tiers.annule}"></strike></c:if>
				</display:column>
				<display:column sortable ="true" titleKey="label.date.ouverture.for" sortProperty="dateOuvertureFor">
					<c:if test="${tiers.annule}"><strike></c:if>
						<fmt:formatDate value="${tiers.dateOuvertureFor}" pattern="dd.MM.yyyy"/>
					<c:if test="${tiers.annule}"></strike></c:if>
				</display:column>
				<display:column sortable ="true" titleKey="label.date.fermeture.for" sortProperty="dateFermetureFor">
					<c:if test="${tiers.annule}"><strike></c:if>
						<fmt:formatDate value="${tiers.dateFermetureFor}" pattern="dd.MM.yyyy"/>
					<c:if test="${tiers.annule}"></strike></c:if>
				</display:column>
			
				<display:column sortable ="false" titleKey="label.ouvrir.vers">
					<c:if test="${!tiers.annule}">
						<c:if test="${urlRetour == null}">
							<select name="AppSelect" onchange="javascript:AppSelect_OnChange(this);" <c:if test="${!tiers.annule}">readonly="true"</c:if> >
								<option value="">---</option>
								<c:if test="${!tiers.debiteurInactif}">
									<option value="<c:out value='${tiers.urlTaoPP}'/>"><fmt:message key="label.TAOPP" /></option>
									<option value="<c:out value='${tiers.urlTaoBA}'/>"><fmt:message key="label.TAOBA" /></option>
									<option value="<c:out value='${tiers.urlTaoIS}'/>"><fmt:message key="label.TAOIS" /></option>
								</c:if>
								<option value="<c:out value='${tiers.urlSipf}'/>"><fmt:message key="label.SIPF" /></option>
								<option value="<c:out value='launchcat.do?numero=' /><c:out value='${tiers.numero}' />" ><fmt:message key="label.CAT" /></option>
							</select>
						</c:if>
						<c:if test="${urlRetour != null}">
							<a href="${urlRetour}${tiers.numero}" class="detail" title="<fmt:message key="label.retour.application.appelante" />">&nbsp;</a>
						</c:if>
					</c:if>
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
