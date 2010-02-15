<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="numeroSrc" value="${param.numeroSrc}" />
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.recherche.debiteur" /></tiles:put>
	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/gestion-debiteurIS.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
  	<tiles:put name="body">
		<c:set var="ligneTableau" value="${1}" scope="request" />
	    <c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
	    
	   	<jsp:include page="../../../../general/pp.jsp" >
			<jsp:param name="page" value="rt" />
			<jsp:param name="path" value="sourcier" />
		</jsp:include>
		
	    <form:form method="post" id="formRechercheTiers">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors  cssClass="error"/>
				<jsp:include page="../../../recherche/form.jsp">
					<jsp:param name="typeRecherche" value="rt-debiteur" />
				</jsp:include>
			</fieldset>
		</form:form>

		<display:table 	name="list" id="debiteur" pagesize="25" requestURI="/rt/list-debiteur.do" class="display" sort="list">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncun.debiteur.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.debiteur.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.debiteurs.trouves" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.debiteurs.trouves" /></span></display:setProperty>

			<display:column sortable ="true" titleKey="label.numero.debiteur" sortProperty="numero" >
				<a href="edit.do?numeroSrc=${numeroSrc}&numeroDpi=${debiteur.numero}&provenance=sourcier"><unireg:numCTB numero="${debiteur.numero}" /></a>
			</display:column>
			<display:column sortable ="true" titleKey="label.nom.raison" >
				<c:out value="${debiteur.nom1}" />
				<c:if test="${debiteur.nom2 != null}">
					<br><c:out value="${debiteur.nom2}" />
				</c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.debiteur.is" >
				<c:if test="${debiteur.roleLigne2 != null}">
					<c:out value="${debiteur.roleLigne2}" />
				</c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.date.naissance" sortProperty="dateNaissance">
				<unireg:date date="${debiteur.dateNaissance}"></unireg:date>
			</display:column>
			<display:column property="localiteOuPays" sortable ="true" titleKey="label.localitePays"  />
		</display:table>
		<table border="0">
			<tr>
				<td>
					<input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="document.location.href='../dossiers-apparentes/edit.do?id=${numeroSrc}';" />
				</td>
			</tr>
		</table>
			
	</tiles:put>
</tiles:insert>
