<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<c:set var="numeroDpi" value="${param.numeroDpi}" />
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.recherche.sourcier" /></tiles:put>
  	<tiles:put name="fichierAide">
		<a href="#" onClick="javascript:ouvrirAide('<c:url value='/docs/gestion-debiteurIS.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	</tiles:put>
  	<tiles:put name="body">
		<c:set var="ligneTableau" value="${1}" scope="request" />
	    <c:set var="ligneTableau" value="${ligneTableau + 1}" scope="request" />
	    
	    <jsp:include page="../../../../general/debiteur.jsp" >
			<jsp:param name="page" value="rt" />
			<jsp:param name="path" value="debiteur" />
		</jsp:include>
	    
	    <form:form method="post" id="formRechercheTiers">
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors  cssClass="error"/>
				<jsp:include page="../../../recherche/form.jsp">
					<jsp:param name="typeRecherche" value="rt-sourcier" />
				</jsp:include>
			</fieldset>
		</form:form>

		<display:table 	name="list" id="row" pagesize="${parametresApp.nbMaxParPage}" requestURI="/rt/list-sourcier.do" class="display" sort="list">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncun.sourcier.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.sourcier.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.sourciers.trouves" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.sourciers.trouves" /></span></display:setProperty>

			<display:column sortable ="true" titleKey="label.numero.contribuable" sortProperty="numero" >
				<a href="edit.do?numeroSrc=${row.numero}&numeroDpi=${numeroDpi}&provenance=debiteur"><unireg:numCTB numero="${row.numero}" /></a>
			</display:column>
			<display:column sortable ="true" titleKey="label.prenom.nom" >
				<c:out value="${row.nom1}" />
				<c:if test="${row.nom2 != null}">
					<br><c:out value="${row.nom2}" />
				</c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.date.naissance" sortProperty="dateNaissance">
				<unireg:date date="${row.dateNaissance}"></unireg:date>
			</display:column>
			<display:column sortable ="true" titleKey="label.mode.imposition" >
				<c:if test="${row.roleLigne2 != null}">
					<c:out value="${row.roleLigne2}" />
				</c:if>
			</display:column>
			<display:column property="localiteOuPays" sortable ="true" titleKey="label.localitePays"  />
			<display:column property="forPrincipal" sortable ="true" titleKey="label.for.principal"  />
			<display:column property="dateOuvertureFor" sortable ="true" titleKey="label.date.ouverture.for" format="{0,date,dd.MM.yyyy}" />
			<display:column property="dateFermetureFor" sortable ="true" titleKey="label.date.fermeture.for" format="{0,date,dd.MM.yyyy}" />
		</display:table>
		
		<table border="0">
			<tr>
				<td>
					<input type="button" value="<fmt:message key="label.bouton.retour" />" onClick="document.location.href='../rapports-prestation/edit.do?id=${numeroDpi}';" />
				</td>
			</tr>
		</table>
		
	</tiles:put>
</tiles:insert>
