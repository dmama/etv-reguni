<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="numeroNonHab" value="${param.numeroNonHab}" />
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.recherche.habitant" /></tiles:put>
  	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/fusion.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
  	<tiles:put name="body">
  	
  		<jsp:include page="../../../../general/pp.jsp">
  			<jsp:param name="page" value="fusion" />
			<jsp:param name="path" value="nonHabitant" />
  		</jsp:include>

		<c:set var="ligneTableau" value="${1}" scope="request" />
	    <c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
	    <form:form method="post" id="formRechercheTiers">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors  cssClass="error"/>
				<jsp:include page="../../../recherche/form.jsp">
					<jsp:param name="typeRecherche" value="fusion-habitant" />
				</jsp:include>
			</fieldset>
		</form:form>

		<display:table 	name="list" id="row" pagesize="25" requestURI="/fusion/list-habitant.do" class="display" sort="list">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncun.habitant.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.habitant.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.habitants.trouves" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.habitants.trouves" /></span></display:setProperty>

			<display:column sortable = "true" titleKey="label.numero.contribuable"  sortProperty="numero" >
				<a href="recap.do?numeroNonHab=${numeroNonHab}&numeroHab=${row.numero}"><unireg:numCTB numero="${row.numero}" /></a>
			</display:column>
			<display:column sortable = "true" titleKey="label.prenom.nom" >
				<c:out value="${row.nom1}" />
				<c:if test="${row.nom2 != null}">
					<br><c:out value="${row.nom2}" />
				</c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.date.naissance" sortProperty="dateNaissance">
				<unireg:date date="${row.dateNaissance}"></unireg:date>
			</display:column>
			<display:column property="localiteOuPays" sortable ="true" titleKey="label.localitePays"  />
		</display:table>
	</tiles:put>
</tiles:insert>
