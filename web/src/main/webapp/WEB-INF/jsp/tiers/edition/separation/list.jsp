<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>

<c:set var="numeroPP1" value="${param.numeroPP1}" />

<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title">
  		<fmt:message key="title.separation.recherche.menage.commun" />
  	</tiles:put>
  	<tiles:put name="fichierAide">
	    <li>
		    <a href="#" onClick="ouvrirAide('<c:url value='/docs/creation-separation.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
	</tiles:put>
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>
	    <form:form method="post" id="formRechercheMenageCommun" commandName="searchCommand">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<c:if test="${searchError != null}">
					<span class="error"><c:out value="${searchError}"/></span>
				</c:if>
				<form:errors  cssClass="error"/>
				<jsp:include page="../../recherche/form.jsp">
					<jsp:param name="typeRecherche" value="separation" />
					<jsp:param name="prefixeEffacer" value="/separation" />
				</jsp:include>
			</fieldset>
		</form:form>

		<display:table 	name="list" id="row" pagesize="25" requestURI="/separation/list.do" class="display" sort="list">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncun.menage.commun.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.menage.commun.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.menages.communs.trouves" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.menages.communs.trouves" /></span></display:setProperty>

			<display:column sortable ="true" titleKey="label.numero.contribuable" sortProperty="numero" >
				<a href="recap.do?numeroCple=${row.numero}"><unireg:numCTB numero="${row.numero}" /></a>
			</display:column>
			<display:column sortable ="true" titleKey="label.prenom.nom" >
				<c:out value="${row.nom1}" />
				<c:if test="${row.nom2 != null}">
					<br><c:out value="${row.nom2}" />
				</c:if>
			</display:column>
			<display:column property="localiteOuPays" sortable ="true" titleKey="label.localitePays"  />
		</display:table>
	</tiles:put>
</tiles:insert>