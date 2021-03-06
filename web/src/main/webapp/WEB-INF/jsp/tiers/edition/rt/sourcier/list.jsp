<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/WEB-INF/jsp/include/common.jsp" %>
<tiles:insert template="/WEB-INF/jsp/templates/template.jsp">

  	<tiles:put name="title"><fmt:message key="title.recherche.sourcier" /></tiles:put>
  	<tiles:put name="fichierAide">
	    <li>
		    <a href="#" onClick="ouvrirAide('<c:url value='/docs/gestion-debiteurIS.pdf'/>');" title="AccessKey: a" accesskey="e">Aide</a>
	    </li>
	</tiles:put>
  	<tiles:put name="body">
		<unireg:nextRowClass reset="1"/>

	    <%--@elvariable id="sourcierCriteriaView" type="ch.vd.unireg.rt.view.SourcierListView"--%>
	    <unireg:bandeauTiers numero="${sourcierCriteriaView.numeroDebiteur}" titre="label.caracteristiques.debiteur.is" showValidation="false" showEvenementsCivils="false" showLinks="false" showAvatar="false" showComplements="true"/>

	    <form:form method="get" id="formRechercheTiers" modelAttribute="sourcierCriteriaView">
		    <form:hidden path="numeroDebiteur"/>
			<fieldset>
				<legend><span><fmt:message key="label.criteres.recherche"/></span></legend>
				<form:errors  cssClass="error"/>
				<jsp:include page="../../../recherche/form.jsp">
					<jsp:param name="typeRecherche" value="rt-sourcier" />
					<jsp:param name="prefixeEffacer" value="/rt/sourcier" />
					<jsp:param name="paramsEffacer" value="numeroDpi:${sourcierCriteriaView.numeroDebiteur}"/>
				</jsp:include>
			</fieldset>
		</form:form>

		<display:table 	name="list" id="row" pagesize="25" requestURI="/rapports-prestation/search-sourcier.do" class="display" sort="list">
			<display:setProperty name="paging.banner.no_items_found"><span class="pagebanner"><fmt:message key="banner.auncun.sourcier.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.one_item_found"><span class="pagebanner">1 <fmt:message key="banner.sourcier.trouve" /></span></display:setProperty>
			<display:setProperty name="paging.banner.some_items_found"><span class="pagebanner">{0} <fmt:message key="banner.sourciers.trouves" /></span></display:setProperty>
			<display:setProperty name="paging.banner.all_items_found"><span class="pagebanner">{0} <fmt:message key="banner.sourciers.trouves" /></span></display:setProperty>

			<display:column sortable ="true" titleKey="label.numero.contribuable" sortProperty="numero" >
				<c:set var="idSourcier">
					<unireg:numCTB numero="${row.numero}"/>
				</c:set>
				<unireg:linkTo name="${idSourcier}" action="/rapports-prestation/add.do" params="{numeroSrc:${row.numero},numeroDpi:${sourcierCriteriaView.numeroDebiteur}}" title="Sélectionner ce contribuable"/>
			</display:column>
			<display:column sortable ="true" titleKey="label.prenom.nom" >
				<c:out value="${row.nom1}" />
				<c:if test="${row.nom2 != null}">
					<br><c:out value="${row.nom2}" />
				</c:if>
			</display:column>
			<display:column sortable ="true" titleKey="label.date.naissance" sortProperty="dateNaissanceInscriptionRC">
				<unireg:date date="${row.dateNaissanceInscriptionRC}"/>
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
					<input type="button" value="<fmt:message key="label.bouton.retour"/>" onclick="Navigation.backTo(['/rapports-prestation/edit.do', '/rapports-prestation/full-list.do'], '/rapports-prestation/edit.do', 'id=${sourcierCriteriaView.numeroDebiteur}')" />
				</td>
			</tr>
		</table>
		
	</tiles:put>
</tiles:insert>
