<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<c:set var="mode" value="${param.mode}" />
<c:set var="population" value="${param.population}" />

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title">
	    <fmt:message key="title.activation.recherche.tiers.${mode}.${population}"/>
  	</tiles:put>
    
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
	    <form:form method="post" id="formRecherche" commandName="searchCommand" action="list.do?mode=${mode}&population=${population}">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors  cssClass="error"/>
				<jsp:include page="../tiers/recherche/form.jsp">
					<jsp:param name="typeRecherche" value="activation-${population}" />
					<jsp:param name="prefixeEffacer" value="/activation" />
					<jsp:param name="paramsEffacer" value="mode:'${mode}',population:'${population}'"/>
				</jsp:include>
			</fieldset>
		</form:form>

	    <c:choose>
		    <c:when test="${searchError != null}">
			    <div class="error"><c:out value="${searchError}"/></div>
			    <c:set var="aucunTiersTrouve"/>
		    </c:when>
			<c:otherwise>
				<c:set var="aucunTiersTrouve">
					<fmt:message key="banner.auncun.tiers.trouve" />
				</c:set>
			</c:otherwise>
	    </c:choose>

		<display:table 	name="list" id="row" pagesize="25" requestURI="/activation/list.do" class="display" sort="list">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><c:out value="${aucunTiersTrouve}"/></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.tiers.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.tiers.trouves" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.tiers.trouves" /></span></display:setProperty>

			<display:column sortable ="true" titleKey="label.numero.tiers" sortProperty="numero" >
				<c:if test="${mode == 'DESACTIVATION'}">
					<a href="../activation/deactivate.do?numero=${row.numero}"><unireg:numCTB numero="${row.numero}" /></a>
				</c:if>
				<c:if test="${mode == 'REACTIVATION'}">
					<a href="../activation/reactivate.do?numero=${row.numero}"><unireg:numCTB numero="${row.numero}" /></a>
				</c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.role" >
				<c:out value="${row.roleLigne1}" />
				<c:if test="${row.roleLigne2 != null}">
					<br><c:out value="${row.roleLigne2}" />
				</c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.nom.raison" >
				<c:out value="${row.nom1}" />
				<c:if test="${row.nom2 != null}">
					<br><c:out value="${row.nom2}" />
				</c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.date.naissance.ou.rc" sortProperty="dateNaissanceInscriptionRC">
				<unireg:date date="${row.dateNaissanceInscriptionRC}"/>
			</display:column>
			<display:column property="localiteOuPays" sortable ="true" titleKey="label.localitePays"  />
			<display:column sortable ="true" titleKey="label.for.principal" >
				<c:out value="${row.forPrincipal}" />
			</display:column>
			<display:column sortable ="true" titleKey="label.date.ouverture.for" sortProperty="dateOuvertureFor">
				<fmt:formatDate value="${row.dateOuvertureFor}" pattern="dd.MM.yyyy"/>
			</display:column>
			<display:column sortable ="true" titleKey="label.date.fermeture.for" sortProperty="dateFermetureFor">
				<fmt:formatDate value="${row.dateFermetureFor}" pattern="dd.MM.yyyy"/>
			</display:column>
		</display:table>
	</tiles:put>
</tiles:insert>